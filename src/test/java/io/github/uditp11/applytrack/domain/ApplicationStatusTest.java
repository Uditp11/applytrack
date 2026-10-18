package io.github.uditp11.applytrack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationStatusTest {

    /** The only legal moves. Every pair not listed here must be refused. */
    private static final Set<String> ALLOWED = Set.of(
            "DRAFT>APPLIED",
            "APPLIED>INTERVIEW", "APPLIED>REJECTED",
            "INTERVIEW>OFFER", "INTERVIEW>REJECTED",
            "DRAFT>WITHDRAWN", "APPLIED>WITHDRAWN", "INTERVIEW>WITHDRAWN");

    static Stream<Arguments> everyPair() {
        return Arrays.stream(ApplicationStatus.values()).flatMap(from ->
                Arrays.stream(ApplicationStatus.values()).map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("everyPair")
    void onlyTheListedMovesAreAllowed(ApplicationStatus from, ApplicationStatus to) {
        assertThat(from.canMoveTo(to)).isEqualTo(ALLOWED.contains(from + ">" + to));
    }

    @Test
    void offerRejectedAndWithdrawnAreFinal() {
        assertThat(Arrays.stream(ApplicationStatus.values()).filter(ApplicationStatus::isFinal))
                .containsExactlyInAnyOrder(
                        ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);
    }
}
