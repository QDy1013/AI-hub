package com.aihub.app.data.db

import androidx.room.TypeConverter
import com.aihub.app.data.model.BillingCycle
import com.aihub.app.data.model.PlanSource
import com.aihub.app.data.model.Platform
import com.aihub.app.data.model.QuotaSource
import com.aihub.app.data.model.SubscriptionStatus

class Converters {
    @TypeConverter fun platformToString(value: Platform): String = value.name
    @TypeConverter fun stringToPlatform(value: String): Platform = Platform.valueOf(value)

    @TypeConverter fun statusToString(value: SubscriptionStatus): String = value.name
    @TypeConverter fun stringToStatus(value: String): SubscriptionStatus = SubscriptionStatus.valueOf(value)

    @TypeConverter fun cycleToString(value: BillingCycle): String = value.name
    @TypeConverter fun stringToCycle(value: String): BillingCycle = BillingCycle.valueOf(value)

    @TypeConverter fun sourceToString(value: QuotaSource): String = value.name
    @TypeConverter fun stringToSource(value: String): QuotaSource = QuotaSource.valueOf(value)

    @TypeConverter fun planSourceToString(value: PlanSource): String = value.name
    @TypeConverter fun stringToPlanSource(value: String): PlanSource = PlanSource.valueOf(value)
}
