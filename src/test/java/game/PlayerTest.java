package game;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import game.Player.Ghost;
import game.Player.RoleBasedAI.Trapper;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(HierarchicalContextRunner.class)
public class PlayerTest implements WithAssertions {

    @Test
    public void tmp() {
        List<Map<String, List<String>>> maps = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        list.add("b1");
        map.put("g1", list);
        map.put("g2", new ArrayList<>());
        maps.add(map);

        map = new HashMap<>();
        map.put("g1", new ArrayList<>());
        list = new ArrayList<>();
        list.add("b1");
        map.put("g2", list);
        maps.add(map);

        List<Map<String, List<String>>> b2 = Trapper.permute("b2", maps);

        System.out.println(b2);
    }

    @Test
    public void set_up() {
        List<String> ghosts = Arrays.asList("g1", "g2");

        List<Map<String, List<String>>> maps = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        for (String ghost : ghosts) {
            map.put(ghost, new ArrayList<>());
        }
        maps.add(map);

        List<Map<String, List<String>>> b2 = Trapper.permute("b1", maps);

        System.out.println(b2);
    }

    @Test
    public void chaining() {
        List<String> ghosts = Arrays.asList("g1", "g2");
        List<String> busters = Arrays.asList("b1", "b2", "b3");

        List<Map<String, List<String>>> maps = new ArrayList<>();
        Map<String, List<String>> map = new HashMap<>();
        for (String ghost : ghosts) {
            map.put(ghost, new ArrayList<>());
        }
        maps.add(map);

        for (String buster : busters) {
            maps = Trapper.permute(buster, maps);
        }

        System.out.println(maps);
    }

}
