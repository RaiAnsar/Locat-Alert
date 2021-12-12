package com.woxich.locatalert.utils

sealed class OrderType {
    object Ascending: OrderType()
    object Descending: OrderType()
}
