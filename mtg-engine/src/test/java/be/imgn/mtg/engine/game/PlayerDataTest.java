package be.imgn.mtg.engine.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerDataTest {

    record TestPlayerId(String id) implements PlayerId {}

    record TestTeam(String name) implements Team {}

    @Nested
    class Construction {

        @Test
        void createsPlayerDataWithIdAndTeam() {
            var id = new TestPlayerId("player1");
            var team = new TestTeam("team1");
            var data = new PlayerData(id, team);

            assertThat(data.id()).isEqualTo(id);
            assertThat(data.team()).isEqualTo(team);
        }
    }

    @Nested
    class RecordEquality {

        @Test
        void equalPlayerDataAreEqual() {
            var id = new TestPlayerId("p1");
            var team = new TestTeam("t1");
            var data1 = new PlayerData(id, team);
            var data2 = new PlayerData(id, team);

            assertThat(data1).isEqualTo(data2);
        }

        @Test
        void differentIdsAreNotEqual() {
            var team = new TestTeam("same");
            var data1 = new PlayerData(new TestPlayerId("p1"), team);
            var data2 = new PlayerData(new TestPlayerId("p2"), team);

            assertThat(data1).isNotEqualTo(data2);
        }

        @Test
        void differentTeamsAreNotEqual() {
            var id = new TestPlayerId("same");
            var data1 = new PlayerData(id, new TestTeam("t1"));
            var data2 = new PlayerData(id, new TestTeam("t2"));

            assertThat(data1).isNotEqualTo(data2);
        }

        @Test
        void hashCodeIsConsistent() {
            var id = new TestPlayerId("p1");
            var team = new TestTeam("t1");
            var data1 = new PlayerData(id, team);
            var data2 = new PlayerData(id, team);

            assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
        }
    }

    @Nested
    class ToString {

        @Test
        void hasDescriptiveToString() {
            var id = new TestPlayerId("player123");
            var team = new TestTeam("teamRed");
            var data = new PlayerData(id, team);

            var str = data.toString();

            assertThat(str).contains("PlayerData");
            assertThat(str).contains("player123");
            assertThat(str).contains("teamRed");
        }
    }
}
