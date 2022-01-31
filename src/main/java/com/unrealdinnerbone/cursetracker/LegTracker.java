//package com.unrealdinnerbone.cursetracker;
//
//import com.google.common.base.Stopwatch;
//import com.unrealdinnerbone.curseapi.api.CurseAPI;
//import com.unrealdinnerbone.curseapi.api.SortOrder;
//import com.unrealdinnerbone.curseapi.api.game.GameVersionType;
//import com.unrealdinnerbone.curseapi.api.mod.Mod;
//import com.unrealdinnerbone.curseapi.api.response.Responses;
//import com.unrealdinnerbone.curseapi.lib.EnvUtil;
//import com.unrealdinnerbone.curseapi.lib.Query;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//import java.util.*;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.stream.Collectors;
//
//public class LegTracker {
//    private static final Logger LOGGER = LoggerFactory.getLogger("Stats");
//    private static final CurseAPI CURSE_API = new CurseAPI("$2a$10$Bj6PfuzSzy9DCLc0f1u.DuHqziqEhyWFTu1cIH4CUcpzTX.xWE3dW");
//    public static final int MINECRAFT_ID = EnvUtil.getInt("MINECRAFT_ID", 432);
//    public static final AtomicInteger COUNT = new AtomicInteger(0);
//
//
//    public static void main(String[] args) throws Exception {
//        Stopwatch stopwatch = Stopwatch.createStarted();
//        updateModsList();
//        LOGGER.info("Time: {}m {}s", stopwatch.stop().elapsed(TimeUnit.SECONDS), stopwatch.elapsed(TimeUnit.MINUTES));
////        Javalin app = Javalin.create().start(1000);
////        app.get("css/stats.css", ctx -> ctx.result(getResourceAsString("css/stats.css")));
////        app.get("js/fixed.js", ctx -> ctx.result(getResourceAsString("js/fixed.js")));
////        app.get("js/sorttable.txt", ctx -> ctx.result(getResourceAsString("js/sorttable.js")));
//
//    }
//
//    public static void updateModsList() throws ExecutionException, InterruptedException, IOException {
//        HashSet<Mod> mods = new HashSet<>();
//        LOGGER.info("Total Mods: {}", CURSE_API.v1().searchMods(new Query.Mod().gameId(MINECRAFT_ID).classId(6)).get().get().pagination().totalCount().orElse(0));
//
//
//        for(GameVersionType datum : CURSE_API.v1().getVersionTypes(MINECRAFT_ID).get().get().data()) {
//            try {
//                mods.addAll(findBlockedMods(0, datum.id(), SortOrder.ASC, 10000));
//            }catch(Exception e) {
//                LOGGER.error("Error while testing {}", datum.id(), e);
//            }
//        }
//
//        LOGGER.info("Total Mods: {}", mods.size());
//        String builder = mods.stream().sorted(Comparator.comparing(Mod::downloadCount)).map(blockedMod -> blockedMod.name() + " - " + blockedMod.downloadCount() + "\n").collect(Collectors.joining());
//        Files.writeString(Path.of("blocked.mods"), builder, StandardOpenOption.TRUNCATE_EXISTING);
//
//
//
//
////            LOGGER.info("Total Mods Found: {}", atomicInteger.get());
//        }
//
////        Data data = JsonUtil.MOSHI.adapter(Data.class).fromJson(Files.readString(Path.of("thing.json")));
////        for(Data.File file : data.files()) {
////            Mod mod = CURSE_API.v1().getMod(file.projectID).get().getExceptionally().data();
////            if(mod.allowModDistribution() != null && !mod.allowModDistribution()) {
////                LOGGER.info("{} {}", mod.name(), false);
////
////            }
////        }
//
//
//    private static Set<Mod> findBlockedMods(int offset, int gameVersion, SortOrder sortOrder, int limit) throws ExecutionException, InterruptedException, IOException {
//        LOGGER.info("Requests Fount: {}", COUNT.incrementAndGet());
//        Responses.SearchMods mods = CURSE_API.v1()
//                .searchMods(new Query.Mod()
//                .gameId(MINECRAFT_ID)
//                .index(offset)
//                .sortOrder(sortOrder)
//                .gameVersionTypeId(gameVersion)
//                .classId(6)).get().get();
//        HashSet<Mod> blockedMods = new HashSet<>();
//        mods.data().stream().distinct().filter(mod -> mod.allowModDistribution() != null && !mod.allowModDistribution()).forEach(blockedMods::add);
//        if(mods.pagination().resultCount() == mods.pagination().pageSize() && offset <= limit){
//            if(mods.pagination().index() == 9950) {
//                if(sortOrder == SortOrder.ASC) {
//                    blockedMods.addAll(findBlockedMods(0, gameVersion, SortOrder.DESC, mods.pagination().totalCount().get() - limit));
//                }
//            }else {
//                blockedMods.addAll(findBlockedMods(offset + 50, gameVersion, sortOrder, limit));
//            }
//        }
//        return blockedMods;
//    }
//
//    public static String getResourceAsString(String thePath) {
//        return new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(thePath), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
//    }
//
//}
