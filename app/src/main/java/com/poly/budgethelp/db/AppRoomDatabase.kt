package com.poly.budgethelp.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.poly.budgethelp.dao.CategoryDao
import com.poly.budgethelp.dao.ProductDao
import com.poly.budgethelp.dao.ReceiptDao
import com.poly.budgethelp.dao.ReceiptWithProductDao
import com.poly.budgethelp.dao.WordToIgnoreDao
import com.poly.budgethelp.data.Category
import com.poly.budgethelp.data.Product
import com.poly.budgethelp.data.Receipt
import com.poly.budgethelp.data.ReceiptProductCrossRef
import com.poly.budgethelp.data.WordToIgnore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(entities = [Product::class, Category::class, Receipt::class, ReceiptProductCrossRef::class, WordToIgnore::class], version = 1, exportSchema = false)
abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun receiptProductDao(): ReceiptWithProductDao
    abstract fun wordToIgnoreDao(): WordToIgnoreDao

    companion object {
        // Singleton prevents multiple instances of the database
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        val DEFAULT_CATEGORY: String = "MÄÄRITTELEMÄTÖN"

        fun getDatabase(context: Context, scope: CoroutineScope): AppRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "app_database"
                ).addCallback(AppDatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch { populateDatabase(database.productDao(),
                    database.categoryDao(), database.receiptDao(), database.receiptProductDao(), database.wordToIgnoreDao()) }
            }
        }

        suspend fun populateDatabase(productDao: ProductDao,
                                     categoryDao: CategoryDao,
                                     receiptDao: ReceiptDao,
                                     receiptProductDao: ReceiptWithProductDao,
                                     wordToIgnoreDao: WordToIgnoreDao) {
            // Clear database
            productDao.deleteAll()
            categoryDao.deleteAll()
            receiptDao.deleteAll()
            receiptProductDao.deleteAll()
            wordToIgnoreDao.deleteAll()

            // Populate with dummy data
            val category0 = Category(DEFAULT_CATEGORY)
            val category1 = Category("RUOKA")
            val product0 = Product("Maito", DEFAULT_CATEGORY, 1.39f)
            val receipt0 = Receipt("Kuitti", 1687881451716, 10.0f)
            val wordToIgnore0 = WordToIgnore("YHTEENSÄ")
            val wordToIgnore1 = WordToIgnore("YHTEENSA")

            categoryDao.insert(category0)
            categoryDao.insert(category1)
            productDao.insert(product0)
            receiptDao.insertReceipt(receipt0)
            wordToIgnoreDao.insert(wordToIgnore0)
            wordToIgnoreDao.insert(wordToIgnore1)

            Log.d("AppRoomDatabase", "Populated database")
        }
    }
}