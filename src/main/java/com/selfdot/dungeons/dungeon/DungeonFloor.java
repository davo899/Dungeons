package com.selfdot.dungeons.dungeon;

import io.github.jdiemke.triangulation.DelaunayTriangulator;
import io.github.jdiemke.triangulation.NotEnoughPointsException;
import io.github.jdiemke.triangulation.Triangle2D;
import io.github.jdiemke.triangulation.Vector2D;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.selfdot.dungeons.DungeonsMod.TILE_SIZE;

public class DungeonFloor {

    private static final Random RANDOM = new Random();
    private static final int MAX_PLACE_ATTEMPTS = 100;
    private static final double GRAPH_DROPOUT = 0.75d;
    private static final double NEW_HALLWAY_PENALTY = 0.75d;

    private final Vector2i size = new Vector2i();
    private final List<PlacedDungeonRoom> rooms = new ArrayList<>();

    public DungeonFloor(int sizeX, int sizeY) {
        this.size.x = sizeX;
        this.size.y = sizeY;
    }

    public boolean placeRandomly(DungeonRoom dungeonRoom) {
        Vector2i roomSize = dungeonRoom.getSize();
        for (int i = 0; i < MAX_PLACE_ATTEMPTS; i++) {
            Vector2i position = new Vector2i(
                RANDOM.nextInt(size.x - roomSize.x),
                RANDOM.nextInt(size.y - roomSize.y)
            );
            if (rooms.stream().noneMatch(
                room -> rectanglesOverlap(position, roomSize, room.position(), room.room().getSize())
            )) {
                rooms.add(new PlacedDungeonRoom(dungeonRoom, position));
                return true;
            }
        }
        return false;
    }

    private Set<RoomEdge> getRoomEdges() {
        Map<Vector2D, PlacedDungeonRoom> roomMap = new HashMap<>();
        rooms.forEach(room -> roomMap.put(room.centre(), room));
        List<Triangle2D> triangles;
        try {
            DelaunayTriangulator delaunayTriangulator = new DelaunayTriangulator(roomMap.keySet().stream().toList());
            delaunayTriangulator.triangulate();
            triangles = delaunayTriangulator.getTriangles();
        } catch (NotEnoughPointsException e) {
            System.out.println("Too few points");
            return new HashSet<>();
        }

        Set<RoomEdge> triangulation = triangles.stream().flatMap(triangle -> Stream.of(
            new RoomEdge(roomMap.get(triangle.a), roomMap.get(triangle.b)),
            new RoomEdge(roomMap.get(triangle.a), roomMap.get(triangle.c)),
            new RoomEdge(roomMap.get(triangle.b), roomMap.get(triangle.c))
        )).collect(Collectors.toSet());

        Set<RoomEdge> minimumSpanningTree = new HashSet<>();
        Set<PlacedDungeonRoom> visited = new HashSet<>();
        Stack<PlacedDungeonRoom> path = new Stack<>();
        path.push(rooms.get(0));
        while (visited.size() < rooms.size()) {
            visited.add(path.peek());
            Optional<PlacedDungeonRoom> next = rooms.stream()
                .filter(room -> !visited.contains(room))
                .filter(room ->
                    triangulation.contains(new RoomEdge(path.peek(), room)) ||
                    triangulation.contains(new RoomEdge(room, path.peek()))
                ).findFirst();
            if (next.isPresent()) {
                minimumSpanningTree.add(new RoomEdge(path.peek(), next.get()));
                path.push(next.get());
            }
            else path.pop();
        }

        triangulation.forEach(edge -> { if (RANDOM.nextDouble() > GRAPH_DROPOUT) minimumSpanningTree.add(edge); });
        return minimumSpanningTree;
    }

    public void connectAllRooms() {
        Set<Vector2i> hallways = new HashSet<>();
        Queue<RoomEdge> roomEdgeQueue = new ArrayDeque<>(getRoomEdges());
        while (!roomEdgeQueue.isEmpty()) {
            RoomEdge edge = roomEdgeQueue.poll();
            PlacedDungeonRoom start = edge.a();
            Set<DungeonPassage> startPassages = start.room().getPassages().stream().map(passage -> new DungeonPassage(
                start.position().x + passage.x(), start.position().y + passage.y(), passage.side()
            )).collect(Collectors.toSet());
            PlacedDungeonRoom end = edge.b();
            Set<DungeonPassage> endPassages = end.room().getPassages().stream().map(passage -> new DungeonPassage(
                end.position().x + passage.x(), end.position().y + passage.y(), passage.side()
            )).collect(Collectors.toSet());

            if (startPassages.stream().map(DungeonPassage::complement).anyMatch(endPassages::contains)) continue;

            PriorityQueue<Stack<Vector2i>> pathFind = new PriorityQueue<>(Comparator.comparingDouble(
                state -> state.size() - (NEW_HALLWAY_PENALTY * state.stream().filter(hallways::contains).count())
            ));
            startPassages.forEach(passage -> {
                Stack<Vector2i> path = new Stack<>();
                DungeonPassage complement = DungeonPassage.complement(passage);
                path.push(new Vector2i(complement.x(), complement.y()));
                pathFind.add(path);
            });
            Set<Vector2i> seen = new HashSet<>();
            while (!pathFind.isEmpty()) {
                Stack<Vector2i> path = pathFind.poll();
                Vector2i position = path.peek();
                if (seen.contains(position)) continue;
                seen.add(position);

                if (Stream.of(
                    new DungeonPassage(position.x(), position.y(), Side.TOP),
                    new DungeonPassage(position.x(), position.y(), Side.BOTTOM),
                    new DungeonPassage(position.x(), position.y(), Side.LEFT),
                    new DungeonPassage(position.x(), position.y(), Side.RIGHT)
                ).map(DungeonPassage::complement).anyMatch(endPassages::contains)) {
                    hallways.addAll(path);
                    break;
                }

                Stream.of(
                    position.add(1, 0, new Vector2i()),
                    position.add(-1, 0, new Vector2i()),
                    position.add(0, 1, new Vector2i()),
                    position.add(0, -1, new Vector2i())
                ).filter(point -> rooms.stream().noneMatch(room -> pointInRectangle(
                    new Vector2f(point.x() + 0.5f, point.y() + 0.5f),
                    room.position(), room.room().getSize()
                ))).forEach(point -> {
                    Stack<Vector2i> nextPath = (Stack<Vector2i>) path.clone();
                    nextPath.push(point);
                    pathFind.add(nextPath);
                });
            }
        }
        hallways.forEach(point -> rooms.add(new PlacedDungeonRoom(new DungeonRoom(1, 1), point)));
    }

    public void placeInWorld(World world, BlockPos corner) {
        Set<DungeonPassage> passages = rooms.stream()
            .flatMap(room -> room.room().getPassages().stream().map(passage -> new DungeonPassage(
                room.position().x() + passage.x(), room.position().y() + passage.y(), passage.side()
            )))
            .collect(Collectors.toSet());
        rooms.forEach(room -> DungeonGenerator.getInstance().addTask(() -> room.room().placeInWorld(
            world,
            corner.add(room.position().x * TILE_SIZE, 0, room.position().y * TILE_SIZE),
            passages,
            room.position()
        )));
    }

    private static boolean rectanglesOverlap(Vector2i pos1, Vector2i size1, Vector2i pos2, Vector2i size2) {
        if (size1.x == 0 || size1.y == 0 || size2.x == 0 || size2.y == 0) return false;
        if (pos1.x > pos2.x + size2.x || pos2.x > pos1.x + size1.x) return false;
        return pos1.y <= pos2.y + size2.y && pos2.y <= pos1.y + size1.y;
    }

    private static boolean pointInRectangle(Vector2f point, Vector2i corner, Vector2i size) {
        return point.x >= corner.x && point.x <= corner.x + size.x &&
            point.y >= corner.y && point.y <= corner.y + size.y;
    }

}
