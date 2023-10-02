package com.fleckinger.tmps.utils

import jakarta.persistence.EntityNotFoundException
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset


class Utils {
    companion object {
        fun getTimezoneId(offset: Int): ZoneId {
            return ZoneId.getAvailableZoneIds().stream()
                .map (ZoneId::of)
                .filter { z -> z.rules.getOffset(Instant.now()).equals(ZoneOffset.ofHours(offset))}
                .findFirst()
                .orElseThrow {EntityNotFoundException("Zone by offset $offset not found")}
        }
    }
}