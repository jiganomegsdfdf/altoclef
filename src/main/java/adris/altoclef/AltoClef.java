package adris.altoclef;

import adris.altoclef.commands.CommandExecutor;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.tasksystem.UserTaskChain;
import adris.altoclef.trackers.*;
import adris.altoclef.util.PlayerExtraController;
import adris.altoclef.util.baritone.BaritoneCustom;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.ClientConnection;

import java.lang.reflect.Field;
import java.util.Objects;

public class AltoClef implements ModInitializer {

    // Central Managers
    private CommandExecutor _commandExecutor;
    private TaskRunner _taskRunner;
    private TrackerManager _trackerManager;
    private ConfigState _configState;
    private BaritoneCustom _baritoneCustom;
    private PlayerExtraController _extraController;

    // Task chains
    private UserTaskChain _userTaskChain;

    // Trackers
    private InventoryTracker _inventoryTracker;
    private EntityTracker _entityTracker;
    private BlockTracker _blockTracker;
    private ContainerTracker _containerTracker;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // As such, nothing will be loaded here but basic initialization.
        StaticMixinHookups.hookupMod(this);
    }

    public void onInitializeLoad() {
        // This code should be run after Minecraft loads everything else in.
        // This is the actual start point, controlled by a mixin.

        // Central Managers
        _commandExecutor = new CommandExecutor(this, "@");
        _taskRunner = new TaskRunner(this);
        _trackerManager = new TrackerManager(this);
        _configState = new ConfigState(this);
        _baritoneCustom = new BaritoneCustom(this, (Baritone)BaritoneAPI.getProvider().getPrimaryBaritone());
        _extraController = new PlayerExtraController(this);

        // Task chains
        _userTaskChain = new UserTaskChain(_taskRunner);

        // Trackers
        _inventoryTracker = new InventoryTracker(_trackerManager);
        _entityTracker = new EntityTracker(_trackerManager);
        _blockTracker = new BlockTracker(_trackerManager);
        _containerTracker = new ContainerTracker(this, _trackerManager);

        initializeCommands();
    }

    // Client tick
    public void onClientTick() {
        _trackerManager.tick();
        _taskRunner.tick();
    }

    // List all command sources here.
    private void initializeCommands() {
        try {
            // This creates the commands. If you want any more commands feel free to initialize new command lists.
            new AltoClefCommands(_commandExecutor);
        } catch (Exception e) {
            /// ppppbbbbttt
            e.printStackTrace();
        }
    }

    /// GETTERS AND SETTERS

    // Main handlers access
    public CommandExecutor getCommandExecutor() {
        return _commandExecutor;
    }
    public TaskRunner getTaskRunner() {
        return _taskRunner;
    }
    //public UserTaskChain getUserTaskChain() { return _userTaskChain; }
    public ConfigState getConfigState() { return _configState; }
    public BaritoneCustom getCustomBaritone() {return _baritoneCustom; }

    // Trackers access
    public InventoryTracker getInventoryTracker() { return _inventoryTracker; }
    public EntityTracker getEntityTracker() { return _entityTracker; }
    public BlockTracker getBlockTracker() { return _blockTracker; }
    public ContainerTracker getContainerTracker() {return _containerTracker;}

    // Baritone access
    public Baritone getClientBaritone() {
        if (getPlayer() == null) {
            return null;
        }
        return (Baritone) BaritoneAPI.getProvider().getBaritoneForPlayer(getPlayer());
    }
    public Settings getClientBaritoneSettings() {
        return Baritone.settings();
    }

    public int getTicks() {
        try {
            ClientConnection con = Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).getConnection();
            Field tickField = ClientConnection.class.getDeclaredField("ticks");
            tickField.setAccessible(true);
            return tickField.getInt(con);
        } catch (NoSuchFieldException | NullPointerException | IllegalAccessException e) {
            return 0;
        }
    }

    // Minecraft access
    public ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
    public ClientPlayerInteractionManager getController() { return MinecraftClient.getInstance().interactionManager; }
    public PlayerExtraController getControllerExtras() {return _extraController; }
    // Extra control
    public void runUserTask(Task task) {
        _userTaskChain.runTask(this, task);
    }
}