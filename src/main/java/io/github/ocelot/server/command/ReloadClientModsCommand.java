package io.github.ocelot.server.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import io.github.ocelot.common.download.ModFileManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * @author Ocelot
 */
public class ReloadClientModsCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("reloadClientMods").requires(commandSource -> commandSource.hasPermission(4)).executes(context ->
        {
            ModFileManager.load();
            return Command.SINGLE_SUCCESS;
        }));
    }
}
