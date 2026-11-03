package com.ntdpz.pie.core.domain.backup

enum class BackupReadFailure { Incomplete, AuthenticationOrDamage, Unsupported, LegacyTooLarge }

class BackupReadException(val reason: BackupReadFailure) : Exception()
