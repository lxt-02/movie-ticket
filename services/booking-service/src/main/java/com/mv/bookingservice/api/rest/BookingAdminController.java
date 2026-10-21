package com.mv.bookingservice.api.rest;

import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.api.dto.response.BookingResponse;
import com.mv.bookingservice.api.mapper.BookingDtoMapper;
import com.mv.bookingservice.application.port.in.GetBookingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Back-office views of bookings (ADMIN / STAFF only, enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/bookings/admin")
@RequiredArgsConstructor
public class BookingAdminController {

    private final GetBookingUseCase getBookingUseCase;
    private final BookingDtoMapper bookingDtoMapper;

    /**
     * Bookings whose money cannot be used (paid late / twice / wrong amount, seats lost after payment).
     * No refund flow in this system: this is the list to handle by hand.
     */
    @GetMapping("/needs-refund")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> needsRefund() {
        List<BookingResponse> bookings = getBookingUseCase.getNeedingRefund().stream()
                .map(bookingDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Booking cần hoàn tiền thủ công: " + bookings.size(), bookings));
    }
}
