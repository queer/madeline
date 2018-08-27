package com.mewna;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Message.Attachment;
import net.dv8tion.jda.core.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author amy
 * @since 8/27/18.
 */
public final class Madeline {
    private static final String POTATO = "\uD83E\uDD54";
    private static final String CHANNEL = System.getenv("CHANNEL");
    private static final String TOKEN = System.getenv("TOKEN");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int THRESHOLD = 1;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DB db = DBMaker.fileDB("madeline.db").make();
    
    private Madeline() {
    }
    
    public static void main(final String[] args) throws LoginException, InterruptedException {
        new Madeline().start();
    }
    
    private void start() throws LoginException, InterruptedException {
        final HTreeMap<String, String> map = db
                .hashMap("messages", Serializer.STRING, Serializer.STRING)
                .createOrOpen();
        
        Runtime.getRuntime().addShutdownHook(new Thread(map::close));
    
        //noinspection AnonymousInnerClassWithTooManyMethods
        new JDABuilder(AccountType.BOT)
                .setToken(TOKEN)
                .addEventListener(new ListenerAdapter() {
                    @Override
                    public void onReady(final ReadyEvent event) {
                        logger.info("Madeline ready!");
                    }
    
                    @Override
                    public void onGuildMessageReactionRemove(final GuildMessageReactionRemoveEvent event) {
                        final ReactionEmote emote = event.getReactionEmote();
                        if(emote.getName().equalsIgnoreCase(POTATO) && emote.getEmote() == null) {
                            final String data = map.get(event.getMessageId());
                            final PotatoMessage potato;
                            if(data != null) {
                                try {
                                    potato = MAPPER.readValue(data, PotatoMessage.class);
                                } catch(final IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                // We just don't even care if the potato doesn't exist
                                return;
                            }
                            potato.reactors.remove(event.getUser().getId());
                            logger.info("Potato exists: {}", map.containsKey(event.getMessageId()));
                            logger.info("Potato data: {}", map.get(event.getMessageId()));
                            if(potato.reactors.size() >= THRESHOLD) {
                                // We just don't even care if the potato doesn't exist
                                if(potato.logId != null) {
                                    // Edit
                                    logger.info("Editing message {} for potato {}", potato.logId, potato.messageId);
                                    event.getJDA().getTextChannelById(CHANNEL).getMessageById(potato.logId)
                                            .queue(msg -> msg.editMessage(
                                                    new MessageBuilder(msg)
                                                            .setContent(String.format("%s **%s**", POTATO,
                                                                    potato.reactors.size()))
                                                            .build()
                                            ).queue());
                                }
                                try {
                                    map.put(event.getMessageId(), MAPPER.writeValueAsString(potato));
                                } catch(final JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                // bad tato, delete
                                map.remove(potato.messageId);
                                event.getJDA().getTextChannelById(CHANNEL).deleteMessageById(potato.logId).queue();
                            }
                        }
                    }
    
                    @Override
                    public void onGuildMessageReactionAdd(final GuildMessageReactionAddEvent event) {
                        final ReactionEmote emote = event.getReactionEmote();
                        final TextChannel channel = event.getChannel();
                        if(emote.getName().equalsIgnoreCase(POTATO) && emote.getEmote() == null) {
                            final Message potatoMessage = channel.getMessageById(event.getMessageId())
                                    .complete();
                            
                            if(potatoMessage.getAuthor().getId().equalsIgnoreCase(event.getUser().getId())) {
                                potatoMessage.getReactions().stream()
                                        .filter(e -> e.getReactionEmote().getName().equalsIgnoreCase(POTATO))
                                        .forEach(e -> e.removeReaction(event.getUser())
                                                .queue(__ -> event.getChannel()
                                                        .sendMessage(event.getUser().getAsMention() + " haha no selftato for you")
                                                        .queue()));
                                return;
                            }
                            
                            final String data = map.get(event.getMessageId());
                            final PotatoMessage potato;
                            if(data != null) {
                                try {
                                    potato = MAPPER.readValue(data, PotatoMessage.class);
                                } catch(final IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                potato = new PotatoMessage(event.getMessageId(), null, channel.getId(), new HashSet<>());
                            }
                            potato.reactors.add(event.getUser().getId());
                            logger.info("Potato exists: {}", map.containsKey(event.getMessageId()));
                            logger.info("Potato data: {}", map.get(event.getMessageId()));
                            if(potato.reactors.size() >= THRESHOLD) {
                                if(potato.logId != null) {
                                    // Edit
                                    logger.info("Editing message {} for potato {}", potato.logId, potato.messageId);
                                    event.getJDA().getTextChannelById(CHANNEL).getMessageById(potato.logId)
                                            .queue(msg -> msg.editMessage(
                                                    new MessageBuilder(msg)
                                                            .setContent(String.format("%s **%s**", POTATO,
                                                                    potato.reactors.size()))
                                                            .build()
                                            ).queue());
                                } else {
                                    // Create
                                    logger.info("Creating message for potato {}", potato.messageId);
                                    String image = null;
                                    if(!potatoMessage.getAttachments().isEmpty()) {
                                        final Attachment attachment = potatoMessage.getAttachments().get(0);
                                        if(attachment.isImage()) {
                                            image = attachment.getProxyUrl();
                                        }
                                    }
                                    final String jumpLink = String.format("https://discordapp.com/channels/%s/%s/%s",
                                            channel.getGuild().getId(),
                                            channel.getId(),
                                            potatoMessage.getId()
                                    );
                                    final Message log = event.getJDA().getTextChannelById(CHANNEL).sendMessage(
                                            new MessageBuilder()
                                                    .setContent(String.format("%s **%s**", POTATO,
                                                            potato.reactors.size()))
                                                    .setEmbed(
                                                            new EmbedBuilder()
                                                                    .setAuthor(potatoMessage.getAuthor().getName(),
                                                                            potatoMessage.getAuthor()
                                                                                    .getEffectiveAvatarUrl())
                                                                    .setDescription(potatoMessage.getContentRaw())
                                                                    .setImage(image)
                                                                    .addField("Jump to message", jumpLink, false)
                                                                    .build()
                                                    )
                                                    .build()
                                    ).complete();
                                    potato.logId = log.getId();
                                }
                            }
                            try {
                                map.put(event.getMessageId(), MAPPER.writeValueAsString(potato));
                            } catch(final JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                })
                .build().awaitReady();
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static final class PotatoMessage {
        private String messageId;
        private String logId;
        private String channelId;
        private Set<String> reactors;
    }
}
