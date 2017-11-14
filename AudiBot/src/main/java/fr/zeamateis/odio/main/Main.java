package fr.zeamateis.odio.main;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.LoginException;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import fr.zeamateis.odio.audio.GuildMusicManager;
import fr.zeamateis.odio.config.Configuration;
import fr.zeamateis.odio.config.ConfigurationBuilder;
import fr.zeamateis.odio.handler.CommandHandler;
import fr.zeamateis.odio.handler.MessagesHandler;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

public class Main extends ListenerAdapter
{
    private final AudioPlayerManager           playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private static JDA                         jda;
    private CommandHandler                     commandHandler;
    private final MessagesHandler              msgHandler = new MessagesHandler();

    public static final String                 NAME       = "ÔdiÔ";
    public static final String                 VERSION    = "0.0.4";
    public static final String                 LOGO       = "https://i.leviathan-studio.com/amateis/if_quacette_2492742.png";

    private Main() {
        this.commandHandler = new CommandHandler(this, this.msgHandler);
        this.musicManagers = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        this.playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        this.playerManager.registerSourceManager(new SoundCloudAudioSourceManager());
        this.playerManager.registerSourceManager(new BandcampAudioSourceManager());
        this.playerManager.registerSourceManager(new VimeoAudioSourceManager());
        this.playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        this.playerManager.registerSourceManager(new HttpAudioSourceManager());
        this.playerManager.registerSourceManager(new LocalAudioSourceManager());
    }

    public synchronized GuildMusicManager getGuildAudioPlayer(Guild guild, TextChannel channel) {
        if (guild != null) {
            long guildId = Long.parseLong(guild.getId());
            GuildMusicManager musicManager = this.musicManagers.get(guildId);

            if (musicManager == null) {
                musicManager = new GuildMusicManager(this.getPlayerManager(), channel);
                this.musicManagers.put(guildId, musicManager);
            }

            guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

            return musicManager;
        }
        else
            return null;
    }

    public AudioPlayerManager getPlayerManager() {
        return this.playerManager;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] command = event.getMessage().getContent().split(" ", 2);
        Guild guild = event.getGuild();
        User author = event.getAuthor();
        TextChannel channel = event.getTextChannel();
        GuildMusicManager musicManager = this.getGuildAudioPlayer(guild, channel);

        if (guild != null)
            switch (command[0]) {
                case "--play":
                    if (command.length == 2)
                        this.commandHandler.loadAndPlay(event, channel, author, command, false);
                    else if (musicManager.player.isPaused())
                        musicManager.player.setPaused(false);
                    else
                        this.msgHandler.wrongCommand(channel);
                    break;
                case "--playnow":
                    if (this.isAdmin(channel))
                        if (command.length == 2)
                            this.commandHandler.loadAndPlay(event, channel, author, command, true);
                        else
                            this.msgHandler.wrongCommand(channel);
                    else
                        this.msgHandler.send(channel, "Vous n'avez pas les droits nécessaires pour cette action");
                    break;
                case "--choose":
                    if (command.length == 2) {
                        AudioTrack track = musicManager.scheduler.getYtSearchList().get(Integer.valueOf(command[1]));
                        this.msgHandler.send(channel, String.format("Musique **%s** ajoutée à la playlist !", track.getInfo().title));
                        musicManager.scheduler.queue(track, author);
                        musicManager.scheduler.getYtSearchList().clear();
                    }
                    break;
                case "--pause":
                    this.commandHandler.pause(channel);
                    break;
                case "--volume":
                    if (this.isAdmin(channel))
                        if (command.length == 2)
                            this.commandHandler.volume(channel, Integer.parseInt(command[1]));
                    break;
                case "--kill":
                    if (author.getName().equals("ZeAmateis"))
                        Main.jda.asBot().getJDA().shutdown(true);
                    break;
                case "--skip":
                    if (this.isAdmin(channel))
                        this.commandHandler.skipTrack(channel);
                    else
                        this.msgHandler.send(channel, "Vous n'avez pas les droits nécessaires pour cette action.\nEssayez **--skipvote**");
                    break;
                case "--stop":
                    this.commandHandler.stop(channel);
                    break;
                case "--mute":
                    this.commandHandler.mute(channel);
                    break;
                case "--unmute":
                    this.commandHandler.unMute(channel);
                    break;
                case "--repeat":
                    this.commandHandler.repeat(channel);
                    break;
                case "--leave":
                    this.commandHandler.leave(channel);
                    break;
                case "--now":
                    this.commandHandler.playlistInfo(channel, false);
                    break;
                case "--join":
                    this.connectToUserChannel(guild.getAudioManager(), author);
                    this.commandHandler.volume(channel, 10);
                    break;
                case "--odio-test":
                    break;
                case "--info":
                    StringBuilder stringBuilder1 = new StringBuilder();
                    stringBuilder1.append(String.format("- Mon créateur est %s et je suis codé en Java !",
                            Main.jda.asBot().getApplicationInfo().complete().getOwner()) + "\n");
                    stringBuilder1.append(String.format("- Mon ping est de __%sms__", Main.jda.getPing()) + "\n");
                    stringBuilder1.append(String.format("- Ma version actuelle est la __%s__", Main.VERSION) + "\n");
                    stringBuilder1.append(String.format("Retrouve moi sur %s", "https://github.com/ZeAmateis/OdiO"));
                    this.msgHandler.wrapMessageInEmbedToAuthor(channel, Color.GREEN, "Infos", stringBuilder1.toString(), author);
                    break;
                case "--help":
                    HashMap<String, String> commandsHelp = new HashMap<>();
                    HashMap<String, String> commandsAdminHelp = new HashMap<>();
                    commandsHelp.put("join", "Pour me faire rentrer dans un channel.");
                    commandsHelp.put("leave", "Pour me faire sortir du channel.");
                    commandsHelp.put("play",
                            "Puis rentrer une url, ou simplement le nom de la musique.\nOu si une musique est en pause cela relancera sa lecture");
                    commandsAdminHelp.put("playnow", "Même commande que \"play\" permettant de lire la musique instantanément.");
                    commandsHelp.put("pause", "Pour mettre en pause ou relancer votre musique.");
                    commandsHelp.put("stop", "Pour stopper simplement la playlist en cours de lecture.");
                    commandsAdminHelp.put("skip", "Pour passer à la musique suivante.");
                    commandsHelp.put("now", "Pour savoir quelle est la musique en cours de lecture.");
                    commandsAdminHelp.put("volume", "Compris entre 0 et 150, réglez le volume d'écoute.");
                    commandsHelp.put("mute/unmute", "Pour activer ou non mes sons.");
                    commandsHelp.put("repeat", "Pour activer ou non la lecture en boucle.");
                    commandsHelp.put("info", "Pour affichier les infos me concernant.");
                    commandsHelp.put("choose", "Pour choisir la musique parmis la liste des cinq propositions.");
                    commandsHelp.put("remove", "Pour supprimer une musique de la playlist en cours de lecture.");
                    commandsHelp.put("next", "Pour savoir quelle va être la prochaine musique écoutée.");

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(String.format("Hey j'suis **%s**, un bot musical.\n", Main.NAME));
                    stringBuilder.append("Voilà la liste de mes commandes disponibles.\n\n");

                    for (Entry<String, String> commands : commandsHelp.entrySet())
                        stringBuilder.append(String.format("__%s__, *%s*\n", commands.getKey(), commands.getValue()));

                    if (!this.isAdmin(channel)) {
                        stringBuilder.append("\n\n__*Voilà la liste de mes commandes administrateurs disponibles.*__\n\n");

                        for (Entry<String, String> commandsAdmin : commandsAdminHelp.entrySet())
                            stringBuilder.append(String.format("__%s__, *%s*\n", commandsAdmin.getKey(), commandsAdmin.getValue()));
                    }
                    List<String> urlSupport = new ArrayList<>();
                    urlSupport.add("Youtube");
                    urlSupport.add("SoundCloud");
                    urlSupport.add("BandCamp");
                    urlSupport.add("Twitch.tv");
                    urlSupport.add("Vimeo");

                    stringBuilder.append("\n__Je peux lire des musiques venues de ces différents sites:__\n");
                    for (String supportedUrl : urlSupport)
                        stringBuilder.append(String.format("*%s*\n", supportedUrl));

                    stringBuilder.append("\nToute mes commandes commencent par \"--\"\n");
                    this.msgHandler.wrapMessageInEmbedToAuthor(channel, Color.CYAN, "Aide", stringBuilder.toString(), author);
                    break;
                case "--remove":
                    if (command.length == 2)
                        this.commandHandler.remove(channel, author, command);
                    else
                        this.msgHandler.wrongCommand(channel);
                    break;
                case "--skipvote":
                    break;
                case "--next":
                    this.commandHandler.playlistInfo(channel, true);
                    break;
                case "--clear":
                    if (this.isAdmin(channel))
                        musicManager.scheduler.clear(channel);
                    else
                        this.msgHandler.send(channel, "Vous n'avez pas les droits nécessaires pour cette action.");
                    break;
            }
        super.onMessageReceived(event);
    }

    public boolean isAdmin(TextChannel channel) {
        for (Member members : channel.getMembers())
            for (Permission perms : members.getPermissions())
                if (members.hasPermission(Permission.ADMINISTRATOR))
                    return true;
                else
                    return false;
        return false;
    }

    public void connectToUserChannel(AudioManager audioManager, User author) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect())
            for (VoiceChannel chans : author.getJDA().getVoiceChannels())
                for (Member members : chans.getMembers())
                    if (members.getEffectiveName().equals(author.getName()))
                        audioManager.openAudioConnection(chans);
    }

    public static void main(String[] args) {
        try {
            try {
                new ConfigurationBuilder(new File("config.properties")).build();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Main.jda = new JDABuilder(AccountType.BOT).setToken(Configuration.TOKEN)
                    .setAutoReconnect(Boolean.parseBoolean(Configuration.AUTO_RECONNECT)).addEventListener(new Main()).buildAsync();
        } catch (LoginException | IllegalArgumentException | RateLimitedException e) {
            e.printStackTrace();
        }

    }

}
