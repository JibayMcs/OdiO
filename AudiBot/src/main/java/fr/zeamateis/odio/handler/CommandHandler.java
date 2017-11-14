package fr.zeamateis.odio.handler;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Queue;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import fr.zeamateis.odio.audio.GuildMusicManager;
import fr.zeamateis.odio.main.Main;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CommandHandler
{
    private Main            main;
    private MessagesHandler msgHandler;

    public CommandHandler(Main mainIn, MessagesHandler msgHandlerIn) {
        this.main = mainIn;
        this.msgHandler = msgHandlerIn;
    }

    public void volume(final TextChannel channelIn, int volumeIn) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channelIn.getGuild(), channelIn);
        musicManager.player.setVolume(volumeIn);
        this.msgHandler.send(channelIn, "Mon volume est maintenant réglé sur *" + String.valueOf(volumeIn) + "%*");
    }

    public void stop(final TextChannel channel) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);
        if (musicManager.player.getPlayingTrack() != null) {
            this.msgHandler.send(channel, String.format("Musique **%s** stoppée !", musicManager.player.getPlayingTrack().getInfo().title));
            musicManager.player.stopTrack();
            musicManager.scheduler.setRepeating(false);
        }
        else
            this.msgHandler.noMusic(channel);
    }

    public void remove(final TextChannel channel, User author, String[] trackName) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);
        Queue<AudioTrack> queue = musicManager.scheduler.getQueue();

        if (!queue.isEmpty())
            for (AudioTrack track : queue)
                if (track != null)
                    if (track.getInfo().title.equals(trackName[1])) {
                        this.msgHandler.send(channel,
                                String.format("**%s** supprimée de la playlist par _%s_ !", track.getInfo().title, author.getName()));
                        queue.remove(track);
                    }
    }

    public void pause(final TextChannel channel) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);

        if (musicManager.player.getPlayingTrack() != null) {
            musicManager.player.setPaused(!musicManager.player.isPaused());
            if (musicManager.player.isPaused())
                this.msgHandler.send(channel, String.format("Musique **%s** mise en pause !", musicManager.player.getPlayingTrack().getInfo().title));
            else
                this.msgHandler.send(channel, String.format("Lecture de **%s** reprise.", musicManager.player.getPlayingTrack().getInfo().title));
        }
        else
            this.msgHandler.noMusic(channel);
    }

    public void mute(final TextChannel channel) {
        Guild guild = channel.getGuild();
        guild.getAudioManager().setSelfMuted(true);
    }

    public void unMute(final TextChannel channel) {
        Guild guild = channel.getGuild();
        guild.getAudioManager().setSelfMuted(false);
    }

    public void repeat(final TextChannel channel) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);

        if (musicManager.player.getPlayingTrack() != null) {
            musicManager.scheduler.setRepeating(!musicManager.scheduler.isRepeating());

            if (musicManager.scheduler.isRepeating())
                this.msgHandler.send(channel,
                        String.format("Je vais répéter en boucle **%s**", musicManager.player.getPlayingTrack().getInfo().title));
            else
                this.msgHandler.send(channel,
                        String.format("J'arrête la répétition de **%s**", musicManager.player.getPlayingTrack().getInfo().title));
        }
        else
            this.msgHandler.noMusic(channel);
    }

    public void leave(final TextChannel channel) {
        channel.getGuild().getAudioManager().closeAudioConnection();
    }

    public void playlistInfo(final TextChannel channel, boolean nextTitle) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);
        AudioTrack track = musicManager.player.getPlayingTrack();

        if (track != null) {
            String duration = null;

            if (!nextTitle) {
                duration = musicManager.scheduler.formatTiming(track.getDuration(), track.getDuration());
                for (Entry<AudioTrack, User> userTrack : musicManager.scheduler.userTracks.entrySet())
                    if (userTrack.getKey().getInfo().title.equals(musicManager.player.getPlayingTrack().getInfo().title))
                        this.msgHandler.send(channel,
                                String.format("**%s** (%s), en cours de lecture.\nAjoutée par *%s*\nURL: %s",
                                        musicManager.player.getPlayingTrack().getInfo().title, duration, userTrack.getValue().getName(),
                                        musicManager.player.getPlayingTrack().getInfo().uri));
            }
            else if (musicManager.scheduler.nextTrackInfo() != null) {
                duration = musicManager.scheduler.formatTiming(musicManager.scheduler.nextTrackInfo().getDuration(),
                        musicManager.scheduler.nextTrackInfo().getDuration());
                this.msgHandler.send(channel,
                        String.format("Musique à suivre **%s** (%s)", musicManager.scheduler.nextTrackInfo().getInfo().title, duration));
            }
            else
                this.msgHandler.send(channel, "Aucune musique dans la playlist.");
        }
        else
            this.msgHandler.noMusic(channel);

    }

    public void loadAndPlay(MessageReceivedEvent event, final TextChannel channel, User author, final String[] track, boolean now) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);

        if (channel.getGuild().getAudioManager().isConnected()) {
            if (!channel.getGuild().getAudioManager().isSelfMuted()) {
                if (track[1].startsWith("https://") || track[1].startsWith("http://"))
                    this.main.getPlayerManager().loadItemOrdered(musicManager, track[1], new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            CommandHandler.this.msgHandler.send(channel,
                                    String.format("Musique **%s** ajoutée à la playlist !", track.getInfo().title));
                            play(musicManager, track, author, now);
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            for (AudioTrack tracks : playlist.getTracks())
                                play(musicManager, tracks, author, now);
                        }

                        @Override
                        public void noMatches() {
                            CommandHandler.this.msgHandler.noMatches(channel, track);
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            CommandHandler.this.msgHandler.loadFailed(channel, exception);
                        }
                    });
                else
                    this.main.getPlayerManager().loadItem("ytsearch: " + Arrays.toString(track), new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {}

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {

                            for (int i = 0; i < 5; i++) {
                                CommandHandler.this.msgHandler.send(channel,
                                        String.format("**[%s]** %s", i, playlist.getTracks().get(i).getInfo().title));
                                musicManager.scheduler.getYtSearchList().add(playlist.getTracks().get(i));
                            }
                        }

                        @Override
                        public void noMatches() {
                            CommandHandler.this.msgHandler.noMatches(channel, track);
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            CommandHandler.this.msgHandler.loadFailed(channel, exception);
                        }
                    });
            }
            else
                this.msgHandler.send(channel, "Humpf... pfumm... Je peux pas parler. **--unmute** moi d'abord.");
        }
        else
            this.msgHandler.send(channel, "Faites moi venir dans le channel avec **--join** d'abord.");
    }

    public void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = this.main.getGuildAudioPlayer(channel.getGuild(), channel);
        if (!musicManager.scheduler.getQueue().isEmpty())
            musicManager.scheduler.nextTrack();
        else
            this.msgHandler.send(channel, "Aucune musique dans la playlist pour passer à la suivante.");
    }

    private static void play(GuildMusicManager musicManager, AudioTrack track, User author, boolean now) {
        if (now)
            musicManager.scheduler.playNow(track, author);
        else
            musicManager.scheduler.queue(track, author);
    }

}
