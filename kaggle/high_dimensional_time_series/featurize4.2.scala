
// spark-shell -i featurize4.2.scala --conf spark.yarn.executor.memoryOverhead=4G --conf spark.yarn.driver.memoryOverhead=4G 

//libraries  
import org.apache.spark._
import spark.implicits._ 
// import util.Random.nextInt 
import org.apache.spark.sql._ // congtains 'Row' 
import org.apache.spark.sql.functions._ 
import java.util.Random 
import scala.util.control.Exception.catching 
import scala.collection.mutable.ListBuffer 
import org.apache.spark.sql.functions.{from_unixtime, unix_timestamp}

// safe casting  
def toBoolSafe ( s : String ) : Option[Boolean] = catching(classOf[IllegalArgumentException]) opt s.toBoolean 
def toDoubleSafe ( s : String ) : Option[Double] = catching(classOf[IllegalArgumentException]) opt s.toDouble 

// Load data  
val idx_date_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/indexed_dates.csv" ).map( x => ( x.getString(0).toInt , x.getString(1) ) ).toDF( "date_idx" , "date" ) 
val items_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/items.csv" ).map( x => ( x.getString(0).toInt , x.getString(1) , x.getString(2).toInt , x.getString(3).toInt ) ).toDF( "item_nbr" , "family" , "class" , "perishable" )  
val oil_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/interpolated_oil.csv" ).map( x => ( x.getString(0) , x.getString(1).toDouble ) ).toDF( "date" , "price" )  
val out_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/sample_submission.csv" ).map( x => ( x.getString(0).toInt , x.getString(1).toDouble ) ).toDF( "id" , "unit_sales" ) 
val stores_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/stores.csv" ).map( x => ( x.getString(0).toInt , x.getString(1) , x.getString(2) , x.getString(3) , x.getString(4).toInt ) ).toDF( "store_nbr" , "city" , "state" , "type" , "cluster" )  
val test_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/test.csv" ).map( x => ( x.getString(0).toInt , x.getString(1) , x.getString(2).toInt , x.getString(3).toInt , toBoolSafe(x.getString(4)))).toDF( "id", "date" , "store_nbr" , "item_nbr" , "onpromotion" ) 
val txns_df = spark.read.option("header", true).csv( "s3://wdurnobucket1/data/transactions.csv" ).map( x => ( x.getString(0) , x.getString(1).toInt , x.getString(2).toInt ) ).toDF( "date" , "store_nbr" , "transactions" ) 
val train_df = spark.read.option( "header", true).csv( "s3://wdurnobucket1/data/train.csv" ).map( x => ( x.getString(0).toInt , x.getString(1) , x.getString(2).toInt , x.getString(3).toInt , x.getString(4).toDouble , toBoolSafe(x.getString(5)) ) ).toDF( "id" , "date" , "store_nbr" , "item_nbr" , "unit_sales" , "onpromotion" ) 
val test_first_day_df = spark.read.option("header",true).csv("s3://wdurnobucket1/data/test_first_day.csv").map( x => ( x.getString(0).toInt , x.getString(1) , x.getString(2).toInt , x.getString(3).toInt , toBoolSafe(x.getString(4)))).toDF( "id", "date" , "store_nbr" , "item_nbr" , "onpromotion" )  

val data_df = train_df.select( 'id , 'date , 'store_nbr , 'item_nbr , 'onpromotion ).union( test_df )  

// register databases 
idx_date_df.createOrReplaceTempView( "idx_date" ) 
items_df.createOrReplaceTempView( "items" ) 
oil_df.createOrReplaceTempView( "oil" ) 
out_df.createOrReplaceTempView( "out" ) 
stores_df.createOrReplaceTempView( "stores" ) 
test_df.createOrReplaceTempView( "test" ) 
txns_df.createOrReplaceTempView( "txns" ) 
train_df.createOrReplaceTempView( "train" ) 
train_df.createOrReplaceTempView( "data" ) 

// psd : ( product , store , date ) 
val item_nbrs = items_df.select( "item_nbr" ).collect().map( x => x(0).toString.toInt )  
val store_nbrs = stores_df.select( "store_nbr" ).collect().map( x => x(0).toString.toInt ) 
val date_idxs = idx_date_df.select( "date_idx" ).collect().map( x => x(0).toString.toInt )  

val max_lag = 101 
val predictive_horizon = date_idxs.length - 16 - 16 
val date_idxs_sliced = date_idxs.slice( max_lag , predictive_horizon ) 

// randomly sample from trainable psds 
val n_trainable_psds = ( item_nbrs.length ) * ( store_nbrs.length ) * ( date_idxs_sliced.length ) 
val SAMPLE_SIZE = 50 // 500000 // 4 verified... /// 100 too many ... wtf... // 50 verified 
val rand = new Random() 
val sample_idxs = sc.parallelize( ( 1 to SAMPLE_SIZE ).map( i => ( i , rand.nextInt( n_trainable_psds ) ) ) ) //  psd_idx , psd_sample_idx 

def get_training_psd( idx:Int ) : ( Int , Int , Int ) = {   
	val i = idx % item_nbrs.length  
	val j = ( idx / item_nbrs.length ) % store_nbrs.length  
	val k = ( idx / ( item_nbrs.length * store_nbrs.length ) ) % date_idxs_sliced.length 
	return ( item_nbrs( i ) , store_nbrs( j ) , date_idxs_sliced( k ) ) 
} 

// store PSDs as uniquely indexed table entires  
val training_psds = sample_idxs.map( x => ( x._1 , get_training_psd( x._2 ) ) ) 
val training_psds_df = training_psds.map( x => ( x._1 , x._2._1 , x._2._2 , x._2._3 ) ).toDF( "psd_idx" , "item_nbr" , "store_nbr" , "date_idx" ) 
training_psds_df.createOrReplaceTempView( "training_psds" ) 
val testing_psds_df = test_first_day_df.join( idx_date_df , "date" ).select( test_first_day_df.col("id").alias("psd_idx") , 'item_nbr , 'store_nbr , 'date_idx )   

// Define feature functions 
///////////////////////////

// log-sales will be pre-computed 
val log_sales_df = spark.sql( "SELECT item_nbr , store_nbr , date , LOG(1.0 + unit_sales) AS log_null FROM train" ) 

// This is the only feature that can be encoded with 'spark.sql'. Others dynamically accept Arrays of (Int,Int,Int)s.  
def feature_encode_unit_sales( psd_df : DataFrame = training_psds_df , delay : Int = 0 ) : DataFrame = { 
	// val tbl_with_nulls = spark.sql( "SELECT training_psds.psd_idx AS psd_idx , LOG( 1.0 + train.unit_sales ) AS log_null FROM training_psds INNER JOIN idx_date ON (training_psds.date_idx + " + delay 
	// + ") = idx_date.date_idx "
	// + "LEFT JOIN train ON train.date = idx_date.date AND train.store_nbr = training_psds.store_nbr AND train.item_nbr = training_psds.item_nbr" ) 
	val psds_with_date_df = psd_df.join( idx_date_df.as("df2") , psd_df.col("date_idx") + delay === $"df2.date_idx" ).select( 'psd_idx , 'item_nbr , 'store_nbr , 'date ) 
	val tbl_with_nulls = psds_with_date_df.join( log_sales_df , psds_with_date_df.col("item_nbr") === log_sales_df.col("item_nbr") && psds_with_date_df.col("store_nbr") === log_sales_df.col("store_nbr") && psds_with_date_df.col("date") === log_sales_df.col("date") , "left" ).select( 'psd_idx , 'log_null )  
	return tbl_with_nulls.select( tbl_with_nulls.col( "psd_idx" ) , when( tbl_with_nulls.col( "log_null" ).isNull , 0.0 ).otherwise( tbl_with_nulls.col( "log_null" ) ) ).toDF( "psd_idx" , "log_" + delay )  
} 

// Accepts a PSD DataFrame as an argument.  
def feature_encode_perishable ( psd_df : DataFrame ) : DataFrame = {  
	psd_df.join( items_df ).where( psd_df.col( "item_nbr" ) === items_df.col( "item_nbr" ) ).select( "psd_idx" , "perishable" ) 
} 

def one_hot_encode ( idx:Int , len:Int ) : Array[Int] = { 
	val ar = Array.fill[Int]( len )(0) 
	ar( idx ) = 1 
	return ar 
} 

def feature_encode_store ( psd_df : DataFrame ) : DataFrame = { 
	// The following line results in Int->String cast errors. 
	// psd_df.select( "psd_idx" , "store_nbr" ).map( x => ( x.getString(0).toInt , one_hot_encode( x.getString(1).toInt - 1 , 54 ) ) ).toDF( "psd_idx" , "store_one_hot" )  
	// The following line does not, only b/c data was mapped through a dataset first. 
	val vecs = psd_df.select( "psd_idx" , "store_nbr" ).map( x => ( x.getInt(0) , one_hot_encode( x.getInt(1)-1 , 54 ) ) ).toDF( "psd_idx", "store_one_hot" ) 
	val exprs = vecs.col("psd_idx") +: (0 to (54-1)).map(i => $"store_one_hot".getItem(i).alias(s"store_$i")) 
	return vecs.select( exprs: _* ) 
} 

// family encoding requires a (String,Int) mapping 
// first, get all families 
val product_families = spark.sql( "SELECT DISTINCT family FROM items" ).collect() // length 33 
val family_map_mut = scala.collection.mutable.Map[String,Int]() 
for ( i <- 1 to 33 ) { 
	family_map_mut( product_families(i-1).getString(0) ) = i-1 
} 
val family_map = family_map_mut.toMap // makes it immutable 
 
def feature_encode_item_family ( psd_df : DataFrame ) : DataFrame = { 
	val idx_fam = psd_df.join( items_df ).where( psd_df.col( "item_nbr" ) === items_df.col( "item_nbr" ) ).select( psd_df.col( "psd_idx" ).as( "psd_idx" ) , items_df.col( "family" ).as( "family" ) ) 
	val vecs = idx_fam.map( x => ( x.getInt(0) , one_hot_encode( family_map( x.getString(1) ) , 33 ) ) ).toDF( "psd_idx" , "family_one_hot" ) 
	val exprs = vecs.col("psd_idx") +: (0 to (33-1)).map(i => $"family_one_hot".getItem(i).alias(s"family_$i")) 
	return vecs.select( exprs: _* )   
} 

// class-encoding also requires a map 
val product_classes = spark.sql( "SELECT DISTINCT class FROM items" ).collect() // length 337 
val class_map_mut = scala.collection.mutable.Map[Int,Int]() 
for ( i <- 1 to 337 ) { 
	class_map_mut( product_classes(i-1).getInt(0) ) = i-1 
} 
val class_map = class_map_mut.toMap 

def feature_encode_item_class ( psd_df : DataFrame ) : DataFrame = { 
	val idx_cls = psd_df.join( items_df ).where( psd_df.col( "item_nbr" ) === items_df.col( "item_nbr" ) ).select( psd_df.col( "psd_idx" ).as( "psd_idx" ) , items_df.col( "class" ).as( "class" ) ) 
	val vecs = idx_cls.map( x => ( x.getInt(0) , one_hot_encode( class_map( x.getInt(1) ) , 337 ) ) ).toDF( "psd_idx" , "class_one_hot" ) 
	val exprs = vecs.col("psd_idx") +: (0 to (54-1)).map(i => $"class_one_hot".getItem(i).alias(s"class_$i")) 
	return vecs.select( exprs: _* ) 
} 

// Total sales by family will be referenced often, so we will pre-construct it. 
// total sales by family, class and date 
val sfcd_df = spark.sql( "SELECT SUM( unit_sales ) AS unit_sales , family , class , date FROM (train JOIN items ON items.item_nbr = train.item_nbr ) GROUP BY family, class, date" )  
sfcd_df.createOrReplaceTempView( "sfcd" ) 
// total sales by family and date 
// val sfd_df = sfcd_df.groupBy( 'family , 'date ).agg( sum('unit_sales).alias("unit_sales") ) 
val sfd_df = spark.sql( "SELECT LOG(1.0 + SUM( unit_sales )) AS unit_sales , family , date FROM sfcd GROUP BY family , date" ) 

def feature_encode_total_sales_by_family ( psd_df : DataFrame , delay : Int = 0 ) : DataFrame = { 
	val psd_date_df = psd_df.join( idx_date_df.as("df2") , psd_df.col("date_idx") + delay === $"df2.date_idx" ).select( 'psd_idx , 'item_nbr , 'date ) 
	val psd_family_date_df = psd_date_df.join( items_df , psd_date_df.col("item_nbr") === items_df.col("item_nbr") ).select( 'psd_idx , 'family , 'date )  
	val nulled_df = psd_family_date_df.join(sfd_df, psd_family_date_df.col("family") === sfd_df.col("family") && psd_family_date_df.col("date") === sfd_df.col("date"),"left" ).select('psd_idx , 'unit_sales ) 
	return nulled_df.map( x => { if( x(1) == null ){ ( x.getInt(0) , 0.0 ) }else{ ( x.getInt(0) , x.getDouble(1) ) } } ).toDF( "psd_idx" , "family_total_" + delay ) 
} 

// Total sales by class also requires some pre-compute. 
// total sales by class and date 
// val scd_df = sfcd_df.groupBy( 'class , 'date ).agg( sum('unit_sales).alias("unit_sales") ) 
val scd_df = spark.sql( "SELECT LOG(1.0 + SUM( unit_sales )) AS unit_sales , class , date FROM sfcd GROUP BY class , date" ) 

def feature_encode_total_sales_by_class( psd_df : DataFrame , delay:Int = 0 ) : DataFrame = { 
	val psd_date_df = psd_df.join( idx_date_df.as("df2") , psd_df.col("date_idx") + delay === $"df2.date_idx" ).select( 'psd_idx , 'item_nbr , 'date ) 
	val psd_class_date_df = psd_date_df.join( items_df , psd_date_df.col("item_nbr") === items_df.col("item_nbr") ).select( 'psd_idx , 'class , 'date ) 
	val nulled_df = psd_class_date_df.join( scd_df , psd_class_date_df.col("class") === scd_df.col("class") && psd_class_date_df.col("date") === scd_df.col("date") , "left" ).select( 'psd_idx , 'unit_sales )
	return nulled_df.map( x => { if( x(1) == null ){ ( x.getInt(0) , 0.0 ) }else{ ( x.getInt(0) , x.getDouble(1) ) } } ).toDF( "psd_idx" , "class_total_" + delay )  
} 

def feature_encode_promotion ( psd_df : DataFrame ) : DataFrame = { 
	val psdd = psd_df.join( idx_date_df , psd_df.col("date_idx") === idx_date_df.col("date_idx") ).select( 'psd_idx , 'item_nbr , 'store_nbr , 'date ) 
	val joined_df = psdd.join( data_df , psdd.col("item_nbr") === data_df.col("item_nbr") && psdd.col("store_nbr") === data_df.col("store_nbr") && psdd.col("date") === data_df.col("date") , "left" ).select( 'psd_idx , 'id , 'onpromotion ) // id column is null only when zero sales were made.   
	return joined_df.map( x => { if( x(2) == null ){ if( x(1) == null ){ ( x.getInt(0) ,-2) }else{ ( x.getInt(0) ,-1) } }else{ if(x.getBoolean(2)){ ( x.getInt(0) , 0 ) }else{ ( x.getInt(0) , 1 ) } } } ).toDF( "psd_idx" , "onpromotion" ) 
} 

def feature_encode_time ( psd_df : DataFrame ) : DataFrame = psd_df.select( 'psd_idx , psd_df.col("date_idx").alias( "time" ) ) 

def feature_encode_day_of_year ( psd_df : DataFrame ) : DataFrame = { 
    val psd_date_df = psd_df.join( idx_date_df , psd_df.col("date_idx") === idx_date_df.col("date_idx") ).select( 'psd_idx , 'date ) 
	// yes, the we do encode for up to 366 days in a year... leap year?  
    val vecs = psd_date_df.select( 'psd_idx , date_format( psd_date_df.col("date") , "D" ) ).map( x => ( x.getInt(0) , one_hot_encode( x.getString(1).toInt-1 , 366 ) ) ).toDF( "psd_idx" , "day_of_year" )  
	val exprs = vecs.col("psd_idx") +: (0 to (366-1)).map(i => $"day_of_year".getItem(i).alias(s"day_of_year_$i")) 
	return vecs.select( exprs:_* ) 
}   

def dayToInt( dayOfWeek : String ) : Int = { 
    dayOfWeek match { 
        case "Sunday"    => 1 
        case "Monday"    => 2 
        case "Tuesday"   => 3 
        case "Wednesday" => 4 
        case "Thursday"  => 5 
        case "Friday"    => 6 
        case "Saturday"  => 7 
    } 
} 

def feature_encode_day_of_week ( psd_df : DataFrame ) : DataFrame = {
    val psd_date_df = psd_df.join( idx_date_df , psd_df.col("date_idx") === idx_date_df.col("date_idx") ).select( 'psd_idx , 'date )
    val day_strings_df = psd_date_df.select( 'psd_idx , date_format( psd_date_df.col("date") , "EEEE").alias("day_of_week") )
    val vecs = day_strings_df.map( x => ( x.getInt(0) , one_hot_encode( dayToInt( x.getString(1) ) - 1 , 7 ) ) ).toDF( "psd_idx" , "day_of_week" ) 
	val exprs = vecs.col("psd_idx") +: (0 to (7-1)).map(i => $"day_of_week".getItem(i).alias(s"day_of_week_$i")) 
	return vecs.select( exprs : _* ) 
}

val oil_map = oil_df.rdd.map( x => ( x.getString(0) , x.getDouble(1) ) ).collectAsMap() 
val date_map = idx_date_df.rdd.map( x => ( x.getInt(0) , x.getString(1) ) ).collectAsMap()   

def feature_encode_oil ( psd_df : DataFrame ) : DataFrame = { 
	// val psd_dates_df = psd_df.join( idx_date_df , psd_df.col("date_idx") === idx_date_df.col("date_idx") ).select( 'psd_idx , 'date ) 
	// return psd_dates_df.join( oil_df , psd_dates_df.col("date") === oil_df.col("date") ).select( 'psd_idx , 'price )  
	def get_price ( date_idx : Int ) : Double = { 
                var idx = date_idx 
                while( ! oil_map.contains( date_map( idx ) ) ) { 
                        idx = idx - 1 
                } 
                return oil_map( date_map( idx ) ) 
        } 
        return psd_df.rdd.map( x => ( x.getInt(0) , get_price( x.getInt(3) ) ) ).toDF( "psd_idx" , "price" ) 
} 

// Construct feature matrices 
/////////////////////////////

def build_y_matrix( psd_df : DataFrame = training_psds_df ) : DataFrame = { 
	var y_matrix_df = feature_encode_unit_sales( psd_df , 1 ) 
	val y_col_names = ListBuffer[String]( "psd_idx" , "log_1" ) 
	for ( i <- 2 to 16 ) { 
		val y_column = feature_encode_unit_sales( psd_df , i ) 
		y_matrix_df = y_matrix_df.join( y_column , "psd_idx" )
		y_col_names += ("log_" + i) 
	} 
	return y_matrix_df.toDF( y_col_names: _* ) 
}
val y_matrix_df = build_y_matrix( training_psds_df )  

def build_design_matrix ( psd_df : DataFrame ) : DataFrame = { 
	// one-off statistics  
	val x_perishable = feature_encode_perishable( psd_df ) 
	val x_store = feature_encode_store( psd_df ) 
	val x_family = feature_encode_item_family( psd_df ) 
	val x_class = feature_encode_item_class( psd_df )  
	val x_promotion = feature_encode_promotion( psd_df ) 
	val x_oil = feature_encode_oil( psd_df ) 
	val x_time = feature_encode_time( psd_df ) 
	val x_day_of_year = feature_encode_day_of_year( psd_df )  
	val x_day_of_week = feature_encode_day_of_week( psd_df ) 
	
	var x = x_perishable.join( x_store , "psd_idx" ) 
	x = x.join( x_family , "psd_idx" ) 
	x = x.join( x_class , "psd_idx" ) 
	x = x.join( x_promotion , "psd_idx" ) 
	x = x.join( x_oil , "psd_idx" ) 
	x = x.join( x_time , "psd_idx" ) 
	x = x.join( x_day_of_year , "psd_idx" ) 
	x = x.join( x_day_of_week , "psd_idx" ) 
	
	// autocorrelative statistics 
	for( val i <- 1 to 9 ) { 
		val log_sales = feature_encode_unit_sales( psd_df , -i ) 
		val ft = feature_encode_total_sales_by_family( psd_df , -i ) 
		val ct = feature_encode_total_sales_by_class( psd_df , -i ) 
		
		x = x.join( log_sales , "psd_idx" ) 
		x = x.join( ft , "psd_idx" ) 
		x = x.join( ct , "psd_idx" ) 
	}  
	    for( val i <- 1 to 10 ) {
        val log_sales = feature_encode_unit_sales( psd_df , -i*10 )
        val ft = feature_encode_total_sales_by_family( psd_df , -i*10 )
        val ct = feature_encode_total_sales_by_class( psd_df , -i*10 )

        x = x.join( log_sales , "psd_idx" )
        x = x.join( ft , "psd_idx" )
        x = x.join( ct , "psd_idx" )
    } 
    return x 
} 
val x_matrix_df = build_design_matrix( training_psds_df )  

y_matrix_df.join( x_matrix_df , "psd_idx" ).write.mode("append").format("com.databricks.spark.csv").option("header", "true").save("s3://wdurnobucket1/training2.csv") 

val testing_matrix_df = build_design_matrix( testing_psds_df  ) 
testing_matrix_df.write.mode("append").format("com.databricks.spark.csv").option("header", "true").save("s3://wdurnobucket1/testing2.csv") 



























