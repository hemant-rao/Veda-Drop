package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        AddressEntity::class,
        BookingEntity::class,
        PartnerServiceEntity::class,
        WalletTransactionEntity::class,
        ChatMessageEntity::class,
        ComplaintEntity::class,
        FavoritePartnerEntity::class
    ],
    version = 5, // §698 — BookingEntity gained addressLat/addressLon (live-tracking destination)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun addressDao(): AddressDao
    abstract fun bookingDao(): BookingDao
    abstract fun partnerServiceDao(): PartnerServiceDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun favoritePartnerDao(): FavoritePartnerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nikhatglow_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default user profiles and mock datasets when DB is created
                            INSTANCE?.let { appDb ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateDefaults(appDb)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaults(db: AppDatabase) {
            // Setup a default customer profile
            db.userDao().insertUser(
                UserEntity(
                    id = "me",
                    name = "Ananya Sharma",
                    email = "ananya.sharma@example.com",
                    role = "customer",
                    kycStatus = "not_started",
                    // Connector model: there is NO customer wallet (the customer
                    // pays the partner directly). Keep this at 0 so no fake money
                    // can ever surface.
                    walletBalancePaise = 0
                )
            )

            // Setup some default customer addresses
            db.addressDao().insertAddress(
                AddressEntity(
                    labelText = "Home",
                    line1 = "Block C, 42, Green Glen Layout",
                    line2 = "Outer Ring Road",
                    city = "Bengaluru",
                    pincode = "560103",
                    lat = 12.9165,
                    lon = 77.6787,
                    isDefault = true
                )
            )
            db.addressDao().insertAddress(
                AddressEntity(
                    labelText = "Office",
                    line1 = "Tower C, RMZ Ecospace",
                    line2 = "Bellandur",
                    city = "Bengaluru",
                    pincode = "560103",
                    lat = 12.9242,
                    lon = 77.6798,
                    isDefault = false
                )
            )

            // Set up basic active partner services so the partner screen starts populated
            db.partnerServiceDao().insertPartnerService(
                PartnerServiceEntity("me_srv_001", "srv_001", "Luxury Haircut & Beard Grooming", "Salon", 49900, 45, true, "Wella Professional range & organic argan oils (Sealed pack).")
            )
            db.partnerServiceDao().insertPartnerService(
                PartnerServiceEntity("me_srv_004", "srv_004", "Deep Tissue Healing Massage", "Massage", 149900, 90, true, "Pure cold-pressed sesame oil & herbal pain relievers.")
            )

            // No seeded wallet transactions — the connector model has no customer
            // wallet (customer pays the partner directly; partner pays only ₹99/mo).
        }
    }
}
