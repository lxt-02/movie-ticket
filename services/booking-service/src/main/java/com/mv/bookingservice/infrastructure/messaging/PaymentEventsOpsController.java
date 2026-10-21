package com.mv.bookingservice.infrastructure.messaging;

import com.mv.bookingservice.api.dto.response.ApiResponse;
import com.mv.bookingservice.domain.model.booking.exception.BookingValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operations on the payment-events consumer (ADMIN / STAFF, see SecurityConfig), through the gateway:
 * <ul>
 *     <li>GET  /api/v1/bookings/admin/payment-events-dlt: is the DLT replay running? is chaos on?</li>
 *     <li>POST /api/v1/bookings/admin/payment-events-dlt/replay: start replaying payment-events.DLT</li>
 *     <li>POST /api/v1/bookings/admin/payment-events-dlt/stop</li>
 *     <li>POST /api/v1/bookings/admin/chaos/payment-events?fail=true|false: DEMO ONLY fault injection</li>
 * </ul>
 * With several booking-service instances behind the load balancer, each call reaches one instance; the DLT replay
 * uses one consumer group, so starting it on any instance is enough. Chaos is per instance.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookings/admin")
@RequiredArgsConstructor
public class PaymentEventsOpsController {

    private final KafkaListenerEndpointRegistry registry;
    private final PaymentEventChaos chaos;

    @GetMapping("/payment-events-dlt")
    public ApiResponse<Map<String, Object>> status() {
        return ApiResponse.success("Trạng thái xử lý payment-events", view());
    }

    @PostMapping("/payment-events-dlt/replay")
    public ApiResponse<Map<String, Object>> replay() {
        MessageListenerContainer container = dltContainer();
        if (!container.isRunning()) {
            container.start();
            log.warn("DLT REPLAY started: re-applying events parked in payment-events.DLT");
        }
        return ApiResponse.success("Đang xử lý lại các event trong payment-events.DLT", view());
    }

    @PostMapping("/payment-events-dlt/stop")
    public ApiResponse<Map<String, Object>> stop() {
        MessageListenerContainer container = dltContainer();
        if (container.isRunning()) {
            container.stop();
            log.warn("DLT REPLAY stopped");
        }
        return ApiResponse.success("Đã dừng xử lý lại DLT", view());
    }

    @PostMapping("/chaos/payment-events")
    public ApiResponse<Map<String, Object>> chaos(@RequestParam("fail") boolean fail) {
        if (!chaos.allowed()) {
            throw new BookingValidationException(
                    "Chế độ demo lỗi đang tắt (đặt booking.demo.chaos-enabled=true để bật cho buổi demo)");
        }
        chaos.setFailing(fail);
        log.warn("CHAOS payment-events failing={}", fail);
        return ApiResponse.success(fail
                ? "Đang giả lập sự cố: mọi payment event sẽ lỗi, được retry rồi chuyển sang DLT"
                : "Đã tắt giả lập sự cố", view());
    }

    private MessageListenerContainer dltContainer() {
        MessageListenerContainer container = registry.getListenerContainer(PaymentEventListener.DLT_REPLAY_LISTENER_ID);
        if (container == null) {
            throw new IllegalStateException("DLT replay listener not registered");
        }
        return container;
    }

    private Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("dltReplayRunning", dltContainer().isRunning());
        view.put("chaosAllowed", chaos.allowed());
        view.put("chaosFailing", chaos.isFailing());
        return view;
    }
}
