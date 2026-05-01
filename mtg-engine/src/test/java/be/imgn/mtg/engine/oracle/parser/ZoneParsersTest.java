package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.PlayerRef;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;

class ZoneParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');
    private static final Subject YOU = Subject.player(PlayerRef.Pronoun.YOU);
    private static final Subject THEY = Subject.player(PlayerRef.Pronoun.THEY);

    // ── Zone ──────────────────────────────────────────────────────────────

    @Nested
    class ZoneParser {

        @Test
        void parsesTheBattlefield() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "the battlefield");
            assertThat(result).isEqualTo(Zone.Shared.BATTLEFIELD);
        }

        @Test
        void parsesExile() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "exile");
            assertThat(result).isEqualTo(Zone.Shared.EXILE);
        }

        @Test
        void parsesYourGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your graveyard");
            assertThat(result).isEqualTo(new Zone.Owned.Graveyard(YOU));
        }

        @Test
        void parsesYourLibrary() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your library");
            assertThat(result).isEqualTo(new Zone.Owned.Library(YOU));
        }

        @Test
        void parsesYourHand() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "your hand");
            assertThat(result).isEqualTo(new Zone.Owned.Hand(YOU));
        }

        @Test
        void parsesTheirLibrary() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "their library");
            assertThat(result).isEqualTo(new Zone.Owned.Library(THEY));
        }

        @Test
        void parsesTheGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "the graveyard");
            assertThat(result).isInstanceOf(Zone.Owned.Graveyard.class);
            assertThat(((Zone.Owned) result).kind()).isEqualTo(Zone.Name.GRAVEYARD);
        }

        @Test
        void parsesBareGraveyard() {
            var result = ZoneParsers.ZONE.parseSkipping(SPACE, "graveyard");
            assertThat(result).isInstanceOf(Zone.Owned.Graveyard.class);
            assertThat(((Zone.Owned) result).kind()).isEqualTo(Zone.Name.GRAVEYARD);
        }
    }

    // ── Zone Destination ──────────────────────────────────────────────────

    @Nested
    class ZoneDestination {

        @Test
        void parsesOntoBattlefield() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "onto the battlefield");
            assertThat(result).isInstanceOf(Zone.Destination.Battlefield.class);
            var dest = (Zone.Destination.Battlefield) result;
            assertThat(dest.tapped()).isFalse();
        }

        @Test
        void parsesOntoBattlefieldTapped() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "onto the battlefield tapped");
            assertThat(result).isInstanceOf(Zone.Destination.Battlefield.class);
            var dest = (Zone.Destination.Battlefield) result;
            assertThat(dest.tapped()).isTrue();
        }

        @Test
        void parsesToBattlefield() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to the battlefield");
            assertThat(result).isInstanceOf(Zone.Destination.Battlefield.class);
        }

        @Test
        void parsesOnTopOfYourLibrary() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "on top of your library");
            assertThat(result).isInstanceOf(Zone.Destination.TopOfLibrary.class);
            var top = (Zone.Destination.TopOfLibrary) result;
            assertThat(top.owner()).isEqualTo(YOU);
        }

        @Test
        void parsesToItsOwnersHand() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to its owner's hand");
            assertThat(result).isInstanceOf(Zone.Destination.ToHand.class);
            var hand = (Zone.Destination.ToHand) result;
            assertThat(hand.owner()).isEqualTo(Subject.possessiveSubject("its", "owner"));
        }

        @Test
        void parsesToYourHand() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "to your hand");
            assertThat(result).isInstanceOf(Zone.Destination.ToHand.class);
            var hand = (Zone.Destination.ToHand) result;
            assertThat(hand.owner()).isEqualTo(YOU);
        }

        @Test
        void parsesIntoYourGraveyard() {
            var result = ZoneParsers.ZONE_DESTINATION.parseSkipping(SPACE, "into your graveyard");
            assertThat(result).isInstanceOf(Zone.Destination.IntoZone.class);
            var into = (Zone.Destination.IntoZone) result;
            assertThat(into.name()).isEqualTo(Zone.Name.GRAVEYARD);
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
            assertThat(from.zone()).isEqualTo(new Zone.Owned.Graveyard(YOU));
        }

        @Test
        void parsesFromExile() {
            var result = ZoneParsers.ZONE_SOURCE.parseSkipping(SPACE, "from exile");
            assertThat(result).isInstanceOf(Zone.Source.FromZone.class);
            var from = (Zone.Source.FromZone) result;
            assertThat(from.zone()).isEqualTo(Zone.Shared.EXILE);
        }

        @Test
        void parsesFromAmong() {
            var result = ZoneParsers.ZONE_SOURCE.parseSkipping(SPACE, "from among");
            assertThat(result).isInstanceOf(Zone.Source.FromAmong.class);
        }
    }
}
