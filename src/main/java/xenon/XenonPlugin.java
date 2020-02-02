package xenon;

import arc.*;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.content.Items;
import mindustry.entities.type.Player;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.Call;
import mindustry.plugin.*;
import mindustry.type.ItemStack;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;
import java.util.Random;
import static mindustry.Vars.logic;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static mindustry.Vars.state;
import static mindustry.Vars.world;
import mindustry.core.GameState;
import mindustry.maps.Map;

public class XenonPlugin extends Plugin{
    private Schematic spawn;
    private final Rules rules = new Rules();
    private final int[][] posCoords = {{69, 420}, {190, 330}, {350, 400}, {400, 305}, {270, 295}, {250, 135}, {80, 140}, {160, 56}, {320, 80}, {440, 65}, {400, 208}};
    
    public int[] randCoords(){
        Random gen = new Random();
        return posCoords[gen.nextInt(posCoords.length)];
    }
    
    @Override
    public void init(){
        rules.pvp = true;
        rules.loadout = ItemStack.list(Items.copper, 500, Items.lead, 400, Items.graphite, 150, Items.metaglass, 150, Items.silicon, 150, Items.plastanium, 50);
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1.1f;
        rules.blockHealthMultiplier = 1.1f;
        rules.canGameOver = false;
        rules.unitBuildSpeedMultiplier = 1f;
        rules.playerDamageMultiplier = 0.75f;
        rules.unitDamageMultiplier = 1.1f;
        rules.playerHealthMultiplier = 1f;
        rules.enemyCoreBuildRadius = 20f;
        //@petus reminder: make sure the teams are seperated properly, rn I can break other peeps blocks and shit 
        
        spawn = Schematics.readBase64("bXNjaAB4nE2P0U7DMAxFb9IlLQMk+ACk/UC+hhderTaMSWtSeRtof4/dpNEiVT3xubZbPKE32CWaI56/Ysrp8LnQXxrwfsln4rBQiucgdJTAQwkvHBc6yS2f0hWvE9PtGL5pvGa+w88xTZExjDn9xntm7MfMMVx+iCe8Ec9ym0KzwAdg0EGPkccpdEq+kkE5Fht38t5yttrd6rFOKNNg3JpU8uK7ZkuvUtnRiy0dSqaRa9bXDtcmu5pTcs2WXJ2rH4q+7LU6w9W/9MVbK3ar9bVmMNQdSmXHP8qlLTg=");
        
        Events.on(PlayerJoin.class, event -> {
            int[] coords = randCoords();
            loadout(event.player, coords[0], coords[1]);
            killTiles(Team.derelict);
            int teamPlayerCount = 0;
            for (Player player : playerGroup.all()){
                if (player.getTeam() == event.player.getTeam()){
                    teamPlayerCount++;
                }
            }
            if (event.player.getTeam() == Team.derelict || teamPlayerCount > 0){ //if their assigned team is full or type derelict
                for (Team team : Team.all()){ //going through teams and players
                    if (!team.active() && !(team == Team.derelict)){
                        event.player.setTeam(team);
                        loadout(event.player, coords[0], coords[1]);
                        return;
                    }
                }
            }
        });
        
        Events.on(PlayerLeave.class, event -> {
            int teamPlayerCount = 0;
            for (Player player : playerGroup.all()){
                if (player.getTeam() == event.player.getTeam()){
                    teamPlayerCount++;
                }
            }
            if(teamPlayerCount <= 1 && !event.player.getTeam().active()){
                killTiles(event.player.getTeam());
                for (Player player : playerGroup.all()){
                    Call.onInfoMessage(player.con, "[lightgray]Team [goldenrod]"+event.player.getTeam().name+" [lightgray]has been [red]eliminated[lightgray].");
                }
            }
        });
    }
    
    void loadout(Player player, int x, int y){
        Schematic.Stile coreTile = spawn.tiles.find(s -> s.block instanceof CoreBlock);
        if(coreTile == null) throw new IllegalArgumentException("Schematic has no core tile. Exiting.");
        int ox = x - coreTile.x, oy = y - coreTile.y;
        spawn.tiles.each(st -> {
            Tile tile = world.tile(st.x + ox, st.y + oy);
            if(tile == null) return;

            Call.onConstructFinish(tile, st.block, -1, st.rotation, player.getTeam(), true);
            if(st.block.posConfig){
                tile.configureAny(Pos.get(tile.x - st.x + Pos.x(st.config), tile.y - st.y + Pos.y(st.config)));
            }else{
                tile.configureAny(st.config);
            }
            if(tile.block() instanceof CoreBlock){
                for(ItemStack stack : state.rules.loadout){
                    Call.transferItemTo(stack.item, stack.amount, tile.drawx(), tile.drawy(), tile);
                }
            }
        });
    }
    
    void killTiles(Team team){
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                Tile tile = world.tile(x, y);
                if(tile.entity != null && tile.getTeam() == team){
                    Time.run(Mathf.random(60f * 5), tile.entity::kill);
                }
            }
        }
    }
    
    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("xenonpvp", "Begin hosting with the Xenon Battle Royale (Hexed) gamemode.", (String[] args) -> {
            if(!state.is(GameState.State.menu)){
                Log.err("Stop the server first.");
                return;
            }
            
            Fi fi = new Fi(".\\config\\maps\\xenonpvp1.msav");
            Map map = new Map(fi, 500, 500, StringMap.of("name", "Xenon PvP"), true, 1, 103);
            
            logic.reset();
            world.loadMap(map, rules);
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
        });
    }
}