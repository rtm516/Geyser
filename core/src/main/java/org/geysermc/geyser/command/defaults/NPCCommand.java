/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.command.defaults;

import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.NpcDialoguePacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.GeyserCommand;
import org.geysermc.geyser.command.GeyserCommandSource;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.AnimalEntity;
import org.geysermc.geyser.entity.type.living.monster.CreeperEntity;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;
import java.util.Random;

public class NPCCommand extends GeyserCommand {
    private final GeyserImpl geyser;

    public NPCCommand(GeyserImpl geyser, String name, String description, String permission) {
        super(name, description, permission);
        this.geyser = geyser;
    }

    @Override
    public void execute(GeyserSession session, GeyserCommandSource sender, String[] args) {
        if (session != null) {
            NpcDialoguePacket npcDialoguePacket = new NpcDialoguePacket();
            npcDialoguePacket.setAction(NpcDialoguePacket.Action.OPEN);
            npcDialoguePacket.setDialogue("This is a test dialogue");
            npcDialoguePacket.setSceneName("test_scene"); // Can be anything
            npcDialoguePacket.setNpcName("Test NPC");

            long entityId = 0l;

            // Get a random animal entity
            // This is just an example, you can use any bedrock entity (so things like item frames don't work)
            List<Entity> entities = session.getEntityCache().getEntities().values().stream().filter(entity -> entity instanceof AnimalEntity).toList();
            Entity entity = entities.get(new Random().nextInt(entities.size()));
            geyser.getLogger().debug("Entity: " + entity);

            // Add NPC data to the entity
            entity.getDirtyMetadata().put(EntityDataTypes.HAS_NPC, true);
            entity.getDirtyMetadata().put(EntityDataTypes.NPC_DATA, "{\"picker_offsets\":{\"scale\":[1.70,1.70,1.70],\"translate\":[0,20,0]},\"portrait_offsets\":{\"scale\":[1.750,1.750,1.750],\"translate\":[-7,50,0]},\"skin_list\":[{\"variant\":0}]}");
            entity.updateBedrockMetadata();

            entityId = entity.getGeyserId();

            // Store the entity ID for closing the dialogue
            session.setNpcId(entityId);

            npcDialoguePacket.setUniqueEntityId(entityId); // Can be any entity
            npcDialoguePacket.setActionJson("[{\"button_name\":\"Districts\",\"data\":[],\"mode\":0,\"text\":\"\",\"type\":1},{\"button_name\":\"My Base\",\"data\":[],\"mode\":0,\"text\":\"\",\"type\":1},{\"button_name\":\"World Spawn\",\"data\":[],\"mode\":0,\"text\":\"\",\"type\":1}]");

            session.sendUpstreamPacket(npcDialoguePacket);
        }
    }

    @Override
    public boolean isExecutableOnConsole() {
        return false;
    }

    @Override
    public boolean isBedrockOnly() {
        return true;
    }
}
