package com.zebpay.zebpaylogger

import com.zebpay.logging.LogFunction

@LogFunction
object Utils{
    fun add(a: Int, b: Int): Int {
        return a + b
    }
}