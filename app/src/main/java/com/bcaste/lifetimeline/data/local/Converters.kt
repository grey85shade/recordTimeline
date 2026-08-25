package com.bcaste.lifetimeline.data.local

import androidx.room.TypeConverter
import com.bcaste.lifetimeline.data.local.entity.EventType

class Converters {
    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)
}
