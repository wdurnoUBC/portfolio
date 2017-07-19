#!/usr/bin/env python2

from abc import ABCMeta, abstractmethod

class AbstractExchangeInterface : 
	__metaclass__ = ABCMeta ; 
	
	@abstractmethod 
	def get_bid_ask() : 
		pass 
	
	@abstractmethod
	def place_market_order() : 
		pass 
	
	@abstractmethod 
	def get_assets() : 
		pass  

