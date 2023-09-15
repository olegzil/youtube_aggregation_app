package com.bluestone.scienceexplorer.database

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class ApplicationControlEntity(@Id var dbId: Long, val defaultClientID: String, val uniqueClientID: String, val appVersion: String)
