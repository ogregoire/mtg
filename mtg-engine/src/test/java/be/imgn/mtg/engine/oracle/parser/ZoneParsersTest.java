package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.Zone;
import be.imgn.mtg.engine.oracle.domain.ZoneName;

class ZoneParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    // ── Zone ──────────────────────────────────────────────────────────────

    @Nested
    class ZoneParser {

        @Test
        void parsesTheBattlefield() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "the battlefield");
            assertThat(result).isInstanceOf(Zone.Battlefield.class);
        }

        @Test
        void parsesExile() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "exile");
            assertThat(result).isInstanceOf(Zone.ExileZone.class);
        }

        @Test
        void parsesYourGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your graveyard");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.possessive()).isEqualTo("your");
            assertThat(named.name()).isEqualTo(ZoneName.GRAVEYARD);
        }

        @Test
        void parsesYourLibrary() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your library");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.possessive()).isEqualTo("your");
            assertThat(named.name()).isEqualTo(ZoneName.LIBRARY);
        }

        @Test
        void parsesYourHand() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your hand");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.possessive()).isEqualTo("your");
            assertThat(named.name()).isEqualTo(ZoneName.HAND);
        }

        @Test
        void parsesTheirLibrary() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "their library");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.possessive()).isEqualTo("their");
            assertThat(named.name()).isEqualTo(ZoneName.LIBRARY);
        }

        @Test
        void parsesTheGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "the graveyard");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.possessive()).isNull();
            assertThat(named.name()).isEqualTo(ZoneName.GRAVEYARD);
        }

        @Test
        void parsesBareGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "graveyard");
            assertThat(result).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) result;
            assertThat(named.name()).isEqualTo(ZoneName.GRAVEYARD);
        }
    }

    // ── Zone Destination ──────────────────────────────────────────────────

    @Nested
    class ZoneDestination {

        @Test
        void parsesOntoBattlefield() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "onto the battlefield");
            assertThat(result).isInstanceOf(Zone.Destination.OntoBattlefield.class);
            var dest = (Zone.Destination.OntoBattlefield) result;
            assertThat(dest.tapped()).isFalse();
        }

        @Test
        void parsesOntoBattlefieldTapped() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "onto the battlefield tapped");
            assertThat(result).isInstanceOf(Zone.Destination.OntoBattlefield.class);
            var dest = (Zone.Destination.OntoBattlefield) result;
            assertThat(dest.tapped()).isTrue();
        }

        @Test
        void parsesToBattlefield() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to the battlefield");
            assertThat(result).isInstanceOf(Zone.Destination.OntoBattlefield.class);
        }

        @Test
        void parsesOnTopOfYourLibrary() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "on top of your library");
            assertThat(result).isInstanceOf(Zone.Destination.TopOfLibrary.class);
            var top = (Zone.Destination.TopOfLibrary) result;
            assertThat(top.possessive()).isEqualTo("your");
        }

        @Test
        void parsesToItsOwnersHand() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to its owner's hand");
            assertThat(result).isInstanceOf(Zone.Destination.ToHand.class);
            var hand = (Zone.Destination.ToHand) result;
            assertThat(hand.description()).isEqualTo("its owner's");
        }

        @Test
        void parsesToYourHand() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to your hand");
            assertThat(result).isInstanceOf(Zone.Destination.ToHand.class);
            var hand = (Zone.Destination.ToHand) result;
            assertThat(hand.description()).isEqualTo("your");
        }

        @Test
        void parsesIntoYourGraveyard() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "into your graveyard");
            assertThat(result).isInstanceOf(Zone.Destination.IntoZone.class);
            var into = (Zone.Destination.IntoZone) result;
            assertThat(into.name()).isEqualTo(ZoneName.GRAVEYARD);
        }
    }

    // ── Zone Source ───────────────────────────────────────────────────────

    @Nested
    class ZoneSource {

        @Test
        void parsesFromYourGraveyard() {
            var result = ZoneParsers.ZONE_SOURCE.parseSkipping(SPACE, "from your graveyard");
            assertThat(result).isInstanceOf(Zone.Source.FromZone.class);
            var from = (Zone.Source.FromZone) result;
            assertThat(from.zone()).isInstanceOf(Zone.Named.class);
            var named = (Zone.Named) from.zone();
            assertThat(named.name()).isEqualTo(ZoneName.GRAVEYARD);
        }

        @Test
        void parsesFromExile() {
            var result = ZoneParsers.ZONE_SOURCE.parseSkipping(SPACE, "from exile");
            assertThat(result).isInstanceOf(Zone.Source.FromZone.class);
            var from = (Zone.Source.FromZone) result;
            assertThat(from.zone()).isInstanceOf(Zone.ExileZone.class);
        }

        @Test
        void parsesFromAmong() {
            var result = ZoneParsers.ZONE_SOURCE.parseSkipping(SPACE, "from among");
            assertThat(result).isInstanceOf(Zone.Source.FromAmong.class);
        }
    }
}
