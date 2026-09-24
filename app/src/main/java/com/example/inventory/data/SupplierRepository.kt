package com.example.inventory.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupplierRepository @Inject constructor(
    private val supplierDao: SupplierDao,
) {
    val suppliers: Flow<List<Supplier>> = supplierDao.observeAll()

    suspend fun createSupplier(name: String, phone: String, notes: String): Supplier {
        val supplier = Supplier(name = name, phone = phone, notes = notes)
        val id = supplierDao.insert(supplier)
        return supplier.copy(id = id)
    }

    suspend fun updateSupplier(supplier: Supplier) {
        supplierDao.update(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) {
        supplierDao.delete(supplier)
    }
}
