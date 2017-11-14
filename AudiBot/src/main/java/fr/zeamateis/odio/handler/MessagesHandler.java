package fr.zeamateis.odio.handler;

import java.awt.Color;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;

import fr.zeamateis.odio.main.Main;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class MessagesHandler
{

    public void noMatches(final TextChannel channel, String[] track) {
        channel.sendMessage("J'ai rien trouvé pour: " + track).queue();
        channel.sendMessage("Ne me tape pas s'il te plait, tu seras gentil <3").queue();
    }

    public void loadFailed(final TextChannel channel, FriendlyException exception) {
        channel.sendMessage("Impossible de lire: " + exception.getMessage()).queue();
        channel.sendMessage("Mais merde j'arrive pas à le lire, wtf. Désolé chef !");
    }

    public void send(final TextChannel channel, final String messageIn) {
        channel.sendMessage(messageIn).queue();
    }

    public static void sendAuthorMessage(User authorIn, MessageEmbed messageIn) {
        authorIn.openPrivateChannel().complete().sendMessage(messageIn).queue();
    }

    public void noMusic(final TextChannel channel) {
        channel.sendMessage("Aucune musique en cours de lecture.").queue();
    }

    public void wrongCommand(final TextChannel channel) {
        channel.sendMessage("Hummm... Mauvaise commande. Essaie \"--help\" pour plus d'infos").queue();
    }

    public void wrapMessageInEmbed(final TextChannel channel, Color colorIn, String title, String message, User author) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(author.getName(), author.getAvatarUrl(), author.getAvatarUrl());
        eb.setFooter(Main.NAME, Main.LOGO);
        eb.setColor(colorIn);
        eb.setDescription(message);
        eb.setTitle(title);
        channel.sendMessage(eb.build()).queue();
    }

    public void wrapMessageInEmbedToAuthor(final TextChannel channel, Color colorIn, String title, String message, User author) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(author.getName(), author.getAvatarUrl(), author.getAvatarUrl());
        eb.setFooter(Main.NAME, Main.LOGO);
        eb.setColor(colorIn);
        eb.setDescription(message);
        eb.setTitle(title);
        sendAuthorMessage(author, eb.build());
    }
}
