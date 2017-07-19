#!/usr/bin/env python2 

import init 
import sys , math , time  

# Configure 
 
if len(sys.argv) > 1 : # data farm header if any arguments are given  
	sys.stdout.write( "t\texc1_bub\texc1_bua\texc2_bub\texc2_bua\texc1_btc\texc1_usd\texc2_btc\texc2_usd\texc1_btc_pos\tarb\tarb_target\tlow\thigh\tbtc_cap\tgood_data\n" ) 

exc2_max_invest = 1.0 # proportion of exc2 holdings allowed to be in btc positions  
exc1_max_invest = 0.7 # proportion of exc1 holdings allowed to be in btc positions 
cost = 4.0*0.002 # four transactions at 0.2% of volume   

buff = 0.002 # Portion of asset price   

# The holdings function must be strictly increasing on R+ in [0,1], and holdings(0) = 0. 
def holdings( arb_percent , a=40.0 ) :  # arb_percent = arb / btcusd  
	return( ( (1.0/(1.0 + math.exp(-math.fabs(arb_percent)*a))) -0.5)*2.0 ) 

#def holdings ( arb , a=5/2 ) : 
#	return( 1 - a/max(arb+a,a) ) 

def btc_cap_fn ( net_investable_btc , current_btc_cap ) :  
	if net_investable_btc >= current_btc_cap + 0.02 : # btc_cap lags behind a growing   
		return( current_btc_cap + 0.01 ) 
	if net_investable_btc < current_btc_cap : # but falls in lock-step  
		return( net_investable_btc ) 
	return( current_btc_cap ) 

# Initialize internal parameters 

arb_target = 0.0 # target hedge size  
arb_target_prop = 0.0 # target hedge proportion  

high = 0.0  
low  = 0.0 

high_count = 0 
low_count = 0 
sticky_bound_count = 20 # how many time steps must break a bound before we observe it?  

btc_cap = 0.0 

bot_state = 'none' # long : bot is buying into the arb, short : bot is buying against the arb, none : bot is not buying 

# Main Loop 
while 1 > 0 : 
	
	# Farm data 
	exc1_bub = -1 
	exc1_bua = -1 
	exc2_bub = -1 
	exc2_bua = -1 
	exc1_btc = -1 
	exc1_usd = -1 
	exc2_btc = -1 
	exc2_usd = -1 
	exc1_btc_pos = -1 # Value can actually be negative 
	exc1_net_val = -1 
	exc1_marg_req = -1 
	exc1_marg_init = -1 
	all_good = True 
	
	time.sleep( 0.5 ) 
	try : 
		r = init.exc1.get_bid_ask() 
		exc1_bub = float(r['bid']) 
		exc1_bua = float(r['ask']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	try : 
		r = init.exc2.get_bid_ask() 
		exc2_bub = float(r['bid']) 
		exc2_bua = float(r['ask']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	try : 
		r = init.exc1.get_assets() 
		exc1_btc = float(r['btc']) 
		exc1_usd = float(r['usd']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	try : 
		r = init.exc2.get_assets() 
		exc2_btc = float(r['btc']) 
		exc2_usd = float(r['usd']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	try : 
		r = init.exc1.get_btcusd_position() 
		if len(r) == 0 : 
			exc1_btc_pos = 0.0 
		else : 
			exc1_btc_pos = float(r[0]['amount']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	try : 
		r = init.exc1.get_btcusd_margin_info() 
		exc1_net_val = float(r['net_value']) 
		exc1_marg_req = float(r['margin_requirement']) 
		exc1_marg_init = float(r['initial_margin']) 
	except : 
		all_good = False 
	time.sleep( 0.5 ) 
	
	arb = -1 
	if exc1_bub > 0.0 and exc2_bua > 0.0 : 
		arb = exc1_bub - exc2_bua 
	
	t = str( time.time() ) 
	
	# Use data 
	if all_good :  
		
		# Get crackin' 
		
		# Calculate accessible assets 
		exc2_btc_cap = (math.floor( exc2_btc * 100.0 ) / 100.0) + (math.floor( 100.0 * exc2_usd / ( exc2_bua * 1.002 ) ) / 100.0) # net in btc 
		exc2_btc_cap = math.floor( exc2_max_invest * exc2_btc_cap * 100.0 ) / 100.0 # adjust for max position 
		exc1_btc_cap = (exc1_usd - exc1_btc_pos * exc1_bub * 0.3) # actual exc1_usd value, very confusing. 0.3 is the initial margin funding value. 
		exc1_btc_cap = (math.floor( 100.0 * exc1_btc_cap / ( exc1_bua * 1.002 ) ) / 100.0) + (math.floor( exc1_btc * 100.0 ) / 100.0) 
		exc1_btc_cap = math.floor( exc2_max_invest * exc1_btc_cap * 100.0 ) / 100.0 # adjust for max position 
		btc_cap = btc_cap_fn( min( exc1_btc_cap , exc2_btc_cap ) , btc_cap ) # This should fix the problem  
		
		# minimum distance a price must traverse for worthwhile profitability  
		# It is the cost of transacting plus a profit margin. 
		profit_gap = (cost + buff)*max(exc1_bua,exc2_bua)  
		
		# update high then low  
		if arb > high : 
			high_count += 1 
		else :
			high_count = 0 
		
		if high_count > sticky_bound_count : 
			high_count = 0 
			high = max( profit_gap , arb ) 
			low = high - profit_gap 
			bot_state = 'short' 
			# calculate holdings target 
			arb_target_prop = holdings( (arb - profit_gap)/max(exc1_bua,exc2_bua) ) # subtraction shifts the curve positively.  
			if arb_target_prop > arb_target : # only increase the target if the proposed target is higher  
				arb_target = math.ceil( arb_target_prop * btc_cap * 100.0 ) / 100.0 # approximate in purchasable units  
		
		# update low then high 
		if arb < low : 
			low_count += 1 
		else : 
			low_count = 0 
		
		if low_count > sticky_bound_count : 
			low_count = 0
			low = arb 
			high = max( profit_gap , low + profit_gap ) 
			bot_state = 'long' 
			# calculate holdings target 
			arb_target_prop = holdings( arb/max(exc1_bua,exc2_bua) ) # no shift, so this long curve is left of the short curve  
			if arb_target_prop < arb_target : # only decrease the target if the proposed target is lower 
				arb_target = math.floor( arb_target_prop * btc_cap * 100.0 ) / 100.0 # approximate in purchasable units  
		
		# Decide whether or not to attempt position adjustment 
		exc1_correction = 0 # take no action 
		exc2_correction = 0 # take no action 
		exc1_short_pos = -exc1_btc_pos 
		exc2_long_pos = math.floor( exc2_btc * 100.0 ) / 100.0 
		if bot_state == 'short' and exc1_short_pos < arb_target and exc1_short_pos < btc_cap and exc1_short_pos - exc2_long_pos <= 0.01 :   
			exc1_correction = -1 # go short 
		if bot_state == 'short' and exc2_long_pos < arb_target and exc2_long_pos < btc_cap and exc2_long_pos - exc1_short_pos <= 0.01 : 
			exc2_correction = 1 # go long 
		if bot_state == 'long' and exc1_short_pos > arb_target and exc1_short_pos > 0.0 and exc2_long_pos - exc1_short_pos <= 0.01 : 
			exc1_correction = 1 # go long 
		if bot_state == 'long' and exc2_long_pos > arb_target and exc2_long_pos > 0.0 and exc1_short_pos - exc2_long_pos <= 0.01 :  
			exc2_correction = -1 # go short 
		if exc1_correction != 0 : 
			sys.stderr.write( "exc1_correction: " + str(exc1_correction) + " at t = " + t + "\n" ) 
		if exc2_correction != 0 : 
			sys.stderr.write( "exc2_correction: " + str(exc2_correction) + " at t = " + t + "\n" ) 
		
		# If decided, attempt position adjustment  
		try : 
			if exc1_correction > 0 : 
				init.exc1.place_market_order( amount =  0.01 ) 
			if exc1_correction < 0 : 
				init.exc1.place_market_order( amount = -0.01 ) 
			if exc2_correction > 0 : 
				init.exc2.place_market_order( amount =  0.01 ) # , price = exc2_bub-20.0 ) 
			if exc2_correction < 0 : 
				init.exc2.place_market_order( amount = -0.01 ) # , price = exc2_bua+20.0 ) 
		except : 
			pass 
		time.sleep( 2.0 ) 
	
	# print out data 
	sys.stdout.write( str(t)+"\t"+str(exc1_bub)+"\t"+str(exc1_bua)+"\t"+str(exc2_bub)+"\t"+str(exc2_bua)+"\t"+str(exc1_btc)
			+"\t"+str(exc1_usd)+"\t"+str(exc2_btc)+"\t"+str(exc2_usd)+"\t"+str(exc1_btc_pos)+"\t"+str(arb)+"\t"
			+str(arb_target)+"\t"+str(low)+"\t"+str(high)+"\t"+str(btc_cap)+"\t"+str(int(all_good))+"\n" ) 



































