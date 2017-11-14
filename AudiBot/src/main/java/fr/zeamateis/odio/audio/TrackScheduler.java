package fr.zeamateis.odio.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import fr.zeamateis.odio.handler.MessagesHandler;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 * This class schedules tracks for the audio player. It contains the queue of
 * tracks.
 */
public class TrackScheduler extends AudioEventAdapter
{
    private boolean                        repeating        = false;
    final AudioPlayer                      player;
    final Queue<AudioTrack>                queue;
    AudioTrack                             lastTrack;
    public final HashMap<AudioTrack, User> userTracks;
    final TextChannel                      channel;
    final MessagesHandler                  messageHandlerIn = new MessagesHandler();
    final List<AudioTrack>                 ytSearchList;

    /**
     * @param player
     *            The audio player this scheduler uses
     */
    public TrackScheduler(AudioPlayer player, TextChannel channel) {
        this.player = player;
        this.queue = new LinkedList<>();
        this.userTracks = new HashMap<>();
        this.channel = channel;
        this.ytSearchList = new ArrayList<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the
     * queue.
     *
     * @param track
     *            The track to play or add to queue.
     */
    public void queue(AudioTrack track, User author) {
        this.userTracks.put(track, author);
        if (!this.player.startTrack(track, true))
            this.queue.offer(track);
    }

    public Queue<AudioTrack> getQueue() {
        return this.queue;
    }

    public List<AudioTrack> getYtSearchList() {
        return this.ytSearchList;
    }

    public void playNow(AudioTrack track, User author) {
        this.queue.clear();
        this.userTracks.put(track, author);
        this.player.startTrack(track, false);
    }

    public void clear(final TextChannel channel) {
        this.player.stopTrack();
        this.queue.clear();
        this.messageHandlerIn.send(channel, "Playlist nettoyée !");
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    public void nextTrack() {
        String duration = this.formatTiming(this.nextTrackInfo().getDuration(), this.nextTrackInfo().getDuration());
        this.messageHandlerIn.send(this.channel, String.format("Écoute en cours de **%s** (%s)", this.nextTrackInfo().getInfo().title, duration));
        this.player.startTrack(this.queue.poll(), false);
    }

    public AudioTrack nextTrackInfo() {
        return this.queue.peek();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        this.lastTrack = track;
        if (endReason.mayStartNext)
            if (this.repeating)
                player.startTrack(this.lastTrack.makeClone(), false);
            else
                this.nextTrack();

    }

    public boolean isRepeating() {
        return this.repeating;
    }

    public void setRepeating(boolean repeating) {
        this.repeating = repeating;
    }

    public void shuffle() {
        Collections.shuffle((List<?>) this.queue);
    }

    public String formatTiming(long timing, long maximum) {
        timing = Math.min(timing, maximum) / 1000;

        long seconds = timing % 60;
        timing /= 60;
        long minutes = timing % 60;
        timing /= 60;
        long hours = timing;

        if (maximum >= 3600000L)
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        else
            return String.format("%d:%02d", minutes, seconds);
    }
}