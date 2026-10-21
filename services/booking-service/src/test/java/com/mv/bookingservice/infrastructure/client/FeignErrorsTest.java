package com.mv.bookingservice.infrastructure.client;

import feign.FeignException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignErrorsTest {

    private static FeignException error(int status, String body) {
        FeignException e = mock(FeignException.class);
        when(e.status()).thenReturn(status);
        when(e.contentUTF8()).thenReturn(body);
        return e;
    }

    @Test
    void readsVietnameseMessageOfOtherService() {
        FeignException e = error(409, "{\"success\":false,\"status\":409,\"message\":\"Ghế A3 vừa có người khác chọn\"}");
        assertEquals("Ghế A3 vừa có người khác chọn", FeignErrors.message(e));
    }

    @Test
    void fallsBackWhenBodyIsMissingOrNotJson() {
        assertNull(FeignErrors.message(error(409, "")));
        assertEquals("mặc định", FeignErrors.messageOr(error(502, "<html>Bad gateway</html>"), "mặc định"));
    }

    @Test
    void unavailableMeansDownOrServerError() {
        assertTrue(FeignErrors.isUnavailable(error(-1, null)));
        assertTrue(FeignErrors.isUnavailable(error(503, null)));
        assertFalse(FeignErrors.isUnavailable(error(401, null)));
        assertFalse(FeignErrors.isUnavailable(error(409, null)));
        assertFalse(FeignErrors.isUnavailable(error(404, null)));
    }
}
