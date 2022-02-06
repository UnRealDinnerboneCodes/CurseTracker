package com.unrealdinnerbone.cursetracker;

import com.squareup.moshi.Moshi;
import com.unrealdinnerbone.cursetracker.temp.RecordsJsonAdapterFactory;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.json.IJsonParser;
import com.unrealdinnerbone.unreallib.json.MoshiParser;
import com.unrealdinnerbone.unreallib.web.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Tracker
{
    private static final IJsonParser<IOException> PARSER = new MoshiParser(new Moshi.Builder().add(new RecordsJsonAdapterFactory()).build());
    private static final Timer TIMER = new Timer();
    private static final Logger LOGGER = LoggerFactory.getLogger(Tracker.class);
    private static final String WEBHOOK_URL = System.getenv("WEBHOOK_URL");

    private static final String BLOCKED_MODS_URL = System.getenv().getOrDefault("BLOCKED_MODS_URL", "https://api.curse.tools/v1/secret/disabledDist");
    private static final String AUTHOR_URL = System.getenv().getOrDefault("AUTHOR_URL", "https://unreal.codes/kevStonk.png");
    private static final String FOLDER_NAME = System.getenv().getOrDefault("FOLDER_NAME", "data");


    public static void main(String[] args)  {
        Path blockedJson = Path.of(FOLDER_NAME).resolve("blocked.json");


        TaskScheduler.scheduleRepeatingTask(1, TimeUnit.HOURS, () -> {
            Queue<DiscordWebhook> webhooks = new ConcurrentLinkedQueue<>();
            try {
                String json = HttpUtils.get(BLOCKED_MODS_URL).body();

                Files.writeString(Path.of(FOLDER_NAME).resolve(System.currentTimeMillis() + ".json"), json, StandardOpenOption.CREATE);

                List<Mod> currentBlocked = getSet(json);
                List<Mod> oldBlocked = getSet(Files.readString(blockedJson));

                currentBlocked.stream()
                        .filter(mod -> !has(oldBlocked, mod))
                        .peek(mod -> LOGGER.info("New blocked mod: {}", mod.name()))
                        .map(mod -> DiscordWebhook.of(WEBHOOK_URL)
                                .setAvatarUrl(AUTHOR_URL)
                                .setContent("❌ [" + mod.name() + "]("+ mod.websiteUrl() + ") has blocked downloads"))
                        .forEach(webhooks::add);


                oldBlocked.stream()
                        .filter(mod -> !has(currentBlocked, mod))
                        .peek(mod -> LOGGER.info("✅ Unblocked mod: {}", mod.name()))
                        .map(mod -> DiscordWebhook.of(WEBHOOK_URL)
                                .setAvatarUrl(AUTHOR_URL)
                                .setContent("✅ [" + mod.name() + "]("+ mod.websiteUrl() + ") has unblocked downloads"))
                        .forEach(webhooks::add);

                Files.writeString(blockedJson, json, StandardOpenOption.TRUNCATE_EXISTING);
            }catch(IOException | InterruptedException e) {
                LOGGER.error("Failed to get blocked mods list", e);
            }

            TIMER.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    DiscordWebhook discordWebhook = webhooks.poll();
                    if (discordWebhook != null) {
                        try {
                            discordWebhook.execute();
                        }catch(IOException | InterruptedException e) {
                            LOGGER.error("Error while sending webhook", e);
                        }
                    }else {
                        LOGGER.info("All webhooks sent");
                        cancel();
                    }
                }

            }, 0, TimeUnit.SECONDS.toMillis(5));
        });






    }


    public static boolean has(List<Mod> mods, Mod mod) {
        return mods.stream().anyMatch(mod1 -> mod.modId == mod1.modId);
    }

    public record Mod(int modId, String name, String author, String websiteUrl, String slug, int downloads, String first_seen) {
        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(!(o instanceof Mod mod)) return false;
            return modId == mod.modId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(modId);
        }
    }


    public static List<Mod> getSet(String json) throws IOException {
        return Arrays.asList(PARSER.parse(Mod[].class, json));
    }
}
