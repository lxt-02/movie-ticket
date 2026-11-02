package com.mv.bookingservice.infrastructure.messaging;

import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordBatchTooLargeException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Sorts a failed Kafka publish into "retry later" or "retrying will not help".
 * <ul>
 *     <li><b>Temporary</b> (retry with back-off, nothing is lost): broker down or unreachable, topic not (yet) in the
 *     metadata, leader election, timeouts. With {@code auto.create.topics.enable=false} a wrong topic name also lands
 *     here ("not present in metadata"), so a typo is visible in {@code last_error} and the rows wait instead of being
 *     delivered to a topic nobody reads.</li>
 *     <li><b>Permanent</b> (row FAILED, operator fixes then requeues): no permission (ACL), bad credentials, record too
 *     large, illegal topic name, value that cannot be serialized. The same message would fail forever.</li>
 * </ul>
 */
public final class KafkaPublishErrors {

    private KafkaPublishErrors() {
    }

    /** The meaningful cause: KafkaTemplate / the future wrap the producer error ("Send failed"). */
    public static Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current
                && (current instanceof ExecutionException
                || current instanceof CompletionException
                || current instanceof org.springframework.kafka.KafkaException
                || current.getClass() == org.apache.kafka.common.KafkaException.class)) {
            current = current.getCause();
        }
        return current;
    }

    public static boolean isPermanent(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof AuthorizationException
                    || t instanceof AuthenticationException
                    || t instanceof RecordTooLargeException
                    || t instanceof RecordBatchTooLargeException
                    || t instanceof InvalidTopicException
                    || t instanceof SerializationException) {
                return true;
            }
        }
        return false;
    }

    /** "TopicAuthorizationException: Not authorized to access topics: [payment-commands]", cut to {@code max}. */
    public static String describe(Throwable error, int max) {
        String text = error.getClass().getSimpleName() + ": " + error.getMessage();
        return text.length() > max ? text.substring(0, max) : text;
    }
}
