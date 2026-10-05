package com.mv.bookingservice.api.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mv.bookingservice.api.dto.request.CreateBookingRequest;
import com.mv.bookingservice.api.dto.request.CreateBookingSeatRequest;
import com.mv.bookingservice.api.exception.GlobalExceptionHandler;
import com.mv.bookingservice.api.mapper.BookingDtoMapper;
import com.mv.bookingservice.application.command.CreateBookingCommand;
import com.mv.bookingservice.application.port.in.CancelBookingUseCase;
import com.mv.bookingservice.application.port.in.CreateBookingUseCase;
import com.mv.bookingservice.application.port.in.GetBookingUseCase;
import com.mv.bookingservice.application.query.GetBookingQuery;
import com.mv.bookingservice.domain.model.booking.aggregate.Booking;
import com.mv.bookingservice.domain.model.booking.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateBookingUseCase createBookingUseCase;

    @Mock
    private GetBookingUseCase getBookingUseCase;

    @Mock
    private CancelBookingUseCase cancelBookingUseCase;

    private final BookingDtoMapper bookingDtoMapper = new BookingDtoMapper();
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        BookingController bookingController = new BookingController(
                createBookingUseCase,
                getBookingUseCase,
                cancelBookingUseCase,
                bookingDtoMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void shouldCreateBookingViaApi() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        UUID holdId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        UUID showtimeSeatId = UUID.randomUUID();

        CreateBookingSeatRequest seatReq = CreateBookingSeatRequest.builder()
                .showtimeSeatId(showtimeSeatId)
                .seatId(seatId)
                .seatLabel("A1")
                .unitPrice(new BigDecimal("120000.00"))
                .build();

        CreateBookingRequest request = CreateBookingRequest.builder()
                .userId(userId)
                .showtimeId(showtimeId)
                .movieTitle("Interstellar")
                .cinemaName("Cinema Central")
                .screenName("Screen IMAX")
                .startTime(Instant.now().plusSeconds(7200))
                .holdId(holdId)
                .currency("VND")
                .discountAmount(BigDecimal.ZERO)
                .seats(List.of(seatReq))
                .build();

        Booking mockBooking = Booking.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK123456")
                .userId(userId)
                .showtimeId(showtimeId)
                .movieTitle("Interstellar")
                .cinemaName("Cinema Central")
                .screenName("Screen IMAX")
                .startTime(Instant.now().plusSeconds(7200))
                .subtotal(new BigDecimal("120000.00"))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("120000.00"))
                .status(BookingStatus.PENDING)
                .currency("VND")
                .createdAt(Instant.now())
                .build();

        when(createBookingUseCase.execute(any(CreateBookingCommand.class))).thenReturn(mockBooking);

        mockMvc.perform(post("/api/v1/bookings")
                        .header("Idempotency-Key", "test-idem-key")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingCode").value("BK123456"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldGetBookingById() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Booking mockBooking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK999999")
                .userId(userId)
                .movieTitle("Oppenheimer")
                .status(BookingStatus.CONFIRMED)
                .totalAmount(new BigDecimal("150000.00"))
                .currency("VND")
                .build();

        when(getBookingUseCase.getById(any(GetBookingQuery.class))).thenReturn(mockBooking);

        mockMvc.perform(get("/api/v1/bookings/" + bookingId)
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(bookingId.toString()))
                .andExpect(jsonPath("$.data.bookingCode").value("BK999999"));
    }
}
