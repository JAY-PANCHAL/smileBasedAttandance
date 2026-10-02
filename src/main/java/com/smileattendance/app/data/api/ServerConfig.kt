package com.smileattendance.app.data.api

/**
 * The Smile Please server address is fixed per deployment (this build ships for GoldStar
 * Jewellery's on-prem server) — a tablet being paired only ever needs its own device code and
 * key from Device Manager, never the server address, so it isn't something an installer can
 * mistype.
 */
object ServerConfig {
    const val DEFAULT_BASE_URL = "http://192.168.50.200:8089"
}
