package com.unrealdinnerbone.cursetracker;

import com.squareup.moshi.Moshi;
import com.unrealdinnerbone.cursetracker.temp.RecordsJsonAdapterFactory;
import com.unrealdinnerbone.unreallib.TaskScheduler;
import com.unrealdinnerbone.unreallib.discord.DiscordWebhook;
import com.unrealdinnerbone.unreallib.discord.EmbedObject;
import com.unrealdinnerbone.unreallib.json.IJsonParser;
import com.unrealdinnerbone.unreallib.json.MoshiParser;
import com.unrealdinnerbone.unreallib.web.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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


        TaskScheduler.scheduleRepeatingTask(12, TimeUnit.HOURS, () -> {
            Queue<DiscordWebhook> webhooks = new ConcurrentLinkedQueue<>();
            try {
                String json = HttpUtils.get(BLOCKED_MODS_URL).body();

                HashSet<Mod> currentBlocked = getSet(json);
                HashSet<Mod> oldBlocked = getSet(Files.readString(blockedJson));

                currentBlocked.stream()
                        .filter(Predicate.not(oldBlocked::contains))
                        .peek(mod -> LOGGER.info("New blocked mod: {}", mod.name()))
                        .map(mod -> DiscordWebhook.of(WEBHOOK_URL)
                                .addEmbed(EmbedObject.builder()
                                        .color(Color.RED)
                                        .author(new EmbedObject.Author(mod.name() + " has blocked downloads", mod.websiteUrl(), AUTHOR_URL))
                                        .build()))
                        .forEach(webhooks::add);


                oldBlocked.stream()
                        .filter(Predicate.not(currentBlocked::contains))
                        .peek(mod -> LOGGER.info("Unblocked mod: {}", mod.name()))
                        .map(mod -> DiscordWebhook.of(WEBHOOK_URL)
                                .addEmbed(EmbedObject.builder()
                                        .color(Color.GREEN)
                                        .author(new EmbedObject.Author(mod.name() + " has unblocked downloads", mod.websiteUrl(), AUTHOR_URL))
                                        .build()))
                        .forEach(webhooks::add);

                Files.writeString(Path.of("blocked.json"), json);
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
            }, 0L, TimeUnit.SECONDS.toMillis(5));
        });






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


    public static HashSet<Mod> getSet(String json) throws IOException {
        return new HashSet<>(Arrays.asList(PARSER.parse(Mod[].class, json)));
    }
}
