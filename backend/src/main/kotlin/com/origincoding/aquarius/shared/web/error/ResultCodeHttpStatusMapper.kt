package com.origincoding.aquarius.shared.web.error

import com.origincoding.aquarius.shared.error.ResultCode
import org.springframework.http.HttpStatus

interface ResultCodeHttpStatusMapper {
    fun mappings(): Map<ResultCode, HttpStatus>
}
