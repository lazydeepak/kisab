package com.susankhya.kisab.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression test for the schema-12 codec round-trip of supply purchases that
 * carry only a purchaseTradeId (M7.6 Supplier Khata). The back-compat schema-11
 * decode previously truncated these records to 5 parts, dropping purchaseTradeId,
 * which left the detail without a source and made loadFarm() return null (the app
 * fell back to the create-farm screen after any supplier purchase).
 */
class DecodeProbeTest {
    @Test
    fun decodeOnDeviceSupplierKhataPayload() {
        val payload = "12\u001Ffarm-c98b7d91-7350-460d-93fc-28414142d0a7\u001FM7GateB\u001F\u001FNPR\u001F\u001Fparty-f4bf350a-c399-47d0-967a-79623db56fa3\u001DSUPPLIER\u001DGateB Supplier\u001D\u001D\u001Ftrade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be\u001DPURCHASE\u001Dparty-f4bf350a-c399-47d0-967a-79623db56fa3\u001D4000000\u001DFeed\u001D2026-08-16T05:40:40.199756Z\u001Fsettlement-ca1770fb-0e2d-4bb8-8452-51941f2707e2\u001Dtrade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be\u001D1500000\u001D\u001D2026-08-16T05:40:40.199756Z\u001Dtrue\u001F\u001F\u001Fsupply-f66ab91e-8b57-485c-8702-983d346bae56\u001DFeed\u001DBAG\u001D\u001F\u001Dsupply-f66ab91e-8b57-485c-8702-983d346bae56\u001D20\u001DBAG\u001D\u001Dtrade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be\u001F\u001F\u001F"

        val farm = FarmPersistenceCodec.decodeOrNull(payload)
        assertNotNull("decode should succeed", farm)

        assertEquals("M7GateB", farm!!.name)
        assertEquals(1, farm.trades.size)
        val trade = farm.trades.first()
        assertEquals("trade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be", trade.id)
        assertEquals(4000000L, trade.totalMinor)
        assertEquals("party-f4bf350a-c399-47d0-967a-79623db56fa3", trade.partyId)

        assertEquals(1, farm.settlements.size)
        val settlement = farm.settlements.first()
        assertEquals("trade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be", settlement.tradeId)
        assertEquals(1500000L, settlement.amountMinor)
        assertEquals(true, settlement.isInitialPayment)

        assertEquals(1, farm.supplies.size)
        assertEquals("Feed", farm.supplies.first().name)

        assertEquals(1, farm.supplyPurchaseDetails.size)
        val detail = farm.supplyPurchaseDetails.first()
        assertEquals(null, detail.transactionId)
        assertEquals("supply-f66ab91e-8b57-485c-8702-983d346bae56", detail.supplyId)
        assertEquals(java.math.BigDecimal(20), detail.quantity)
        assertEquals("trade-53d9e0a9-5932-4f7d-8d59-d89c6d7225be", detail.purchaseTradeId)
    }
}
