package io.github.ocelot.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import io.github.ocelot.common.download.ModFileManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

/**
 * @author Ocelot
 */
public class ReloadClientModsCommand
{
    public static void register(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(Commands.literal("reloadClientMods").requires(commandSource -> commandSource.hasPermissionLevel(4)).executes(context ->
        {
            ModFileManager.load();
            return Command.SINGLE_SUCCESS;
        }));
    }
}
