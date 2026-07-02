package com.nocrates.crate;

import com.nocrates.item.ItemSpec;
import com.nocrates.key.KeyLink;
import com.nocrates.reward.GuaranteedWin;
import com.nocrates.reward.Reward;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A crate definition — one file in crates/. Mutable so the in-game editor can work on it. */
public final class Crate {

    public enum EngineType {
        /** A real block (chests get the native lid animation). */
        BLOCK,
        /** A floating item model rendered with an ItemDisplay entity. */
        MODEL
    }

    private final String id;
    private boolean enabled = true;
    private String displayName;
    private EngineType engine = EngineType.BLOCK;
    private String blockMaterial = "CHEST";
    private ItemSpec modelItem = new ItemSpec("CHEST");
    private double modelYOffset = 0.0;
    private float modelYaw = 0f;
    private final Set<String> locations = new LinkedHashSet<>();
    private boolean permissionRequired;
    private String permission;
    private final OpenSettings open = new OpenSettings();
    private List<KeyLink> keys = new ArrayList<>();
    private List<String> hologramLines = new ArrayList<>();
    private double hologramOffset = 1.6;
    private boolean previewEnabled = true;
    private String previewMenu = "preview";
    private RewardsMode rewardsMode = RewardsMode.RANDOM;
    private int maxWinRewards = 1;
    private final CrateAnimationConfig animation = new CrateAnimationConfig();
    private final Map<String, Reward> rewards = new LinkedHashMap<>();
    private boolean guaranteedEnabled;
    private GuaranteedWin.Mode guaranteedMode = GuaranteedWin.Mode.REPETITIVE;
    private List<GuaranteedWin.Milestone> milestones = new ArrayList<>();
    private boolean rerollEnabled;
    private int rerollFree;
    private Map<String, Integer> rerollGroups = new LinkedHashMap<>();
    private String broadcastMessage;

    public Crate(String id) {
        this.id = id.toLowerCase(java.util.Locale.ROOT);
        this.displayName = "<gold>" + id + "</gold>";
        this.permission = "nocrates.crate." + this.id;
    }

    public String id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public EngineType engine() {
        return engine;
    }

    public void engine(EngineType engine) {
        this.engine = engine;
    }

    public String blockMaterial() {
        return blockMaterial;
    }

    public void blockMaterial(String blockMaterial) {
        this.blockMaterial = blockMaterial;
    }

    public ItemSpec modelItem() {
        return modelItem;
    }

    public void modelItem(ItemSpec modelItem) {
        this.modelItem = modelItem;
    }

    public double modelYOffset() {
        return modelYOffset;
    }

    public void modelYOffset(double modelYOffset) {
        this.modelYOffset = modelYOffset;
    }

    public float modelYaw() {
        return modelYaw;
    }

    public void modelYaw(float modelYaw) {
        this.modelYaw = modelYaw;
    }

    /** Serialized block locations: "world;x;y;z". */
    public Set<String> locations() {
        return locations;
    }

    public boolean permissionRequired() {
        return permissionRequired;
    }

    public void permissionRequired(boolean permissionRequired) {
        this.permissionRequired = permissionRequired;
    }

    public String permission() {
        return permission;
    }

    public void permission(String permission) {
        this.permission = permission;
    }

    public OpenSettings open() {
        return open;
    }

    /** Key requirements; empty = keyless crate. */
    public List<KeyLink> keys() {
        return keys;
    }

    public void keys(List<KeyLink> keys) {
        this.keys = new ArrayList<>(keys);
    }

    public List<String> hologramLines() {
        return hologramLines;
    }

    public void hologramLines(List<String> hologramLines) {
        this.hologramLines = new ArrayList<>(hologramLines);
    }

    public double hologramOffset() {
        return hologramOffset;
    }

    public void hologramOffset(double hologramOffset) {
        this.hologramOffset = hologramOffset;
    }

    public boolean previewEnabled() {
        return previewEnabled;
    }

    public void previewEnabled(boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public String previewMenu() {
        return previewMenu;
    }

    public void previewMenu(String previewMenu) {
        this.previewMenu = previewMenu;
    }

    public RewardsMode rewardsMode() {
        return rewardsMode;
    }

    public void rewardsMode(RewardsMode rewardsMode) {
        this.rewardsMode = rewardsMode;
    }

    public int maxWinRewards() {
        return maxWinRewards;
    }

    public void maxWinRewards(int maxWinRewards) {
        this.maxWinRewards = Math.max(1, Math.min(30, maxWinRewards));
    }

    public CrateAnimationConfig animation() {
        return animation;
    }

    public Map<String, Reward> rewards() {
        return rewards;
    }

    public List<Reward> rewardList() {
        return new ArrayList<>(rewards.values());
    }

    public Reward reward(String id) {
        return id == null ? null : rewards.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public void addReward(Reward reward) {
        rewards.put(reward.id().toLowerCase(java.util.Locale.ROOT), reward);
    }

    public void removeReward(String id) {
        rewards.remove(id.toLowerCase(java.util.Locale.ROOT));
    }

    /** Normalized chance (0-100) of one reward relative to the whole crate. */
    public double normalizedChance(Reward reward) {
        double total = rewards.values().stream().mapToDouble(Reward::percentage).sum();
        return total <= 0 ? 0 : reward.percentage() / total * 100.0;
    }

    public boolean guaranteedEnabled() {
        return guaranteedEnabled;
    }

    public void guaranteedEnabled(boolean guaranteedEnabled) {
        this.guaranteedEnabled = guaranteedEnabled;
    }

    public GuaranteedWin.Mode guaranteedMode() {
        return guaranteedMode;
    }

    public void guaranteedMode(GuaranteedWin.Mode guaranteedMode) {
        this.guaranteedMode = guaranteedMode;
    }

    public List<GuaranteedWin.Milestone> milestones() {
        return milestones;
    }

    public void milestones(List<GuaranteedWin.Milestone> milestones) {
        this.milestones = new ArrayList<>(milestones);
    }

    public boolean rerollEnabled() {
        return rerollEnabled;
    }

    public void rerollEnabled(boolean rerollEnabled) {
        this.rerollEnabled = rerollEnabled;
    }

    public int rerollFree() {
        return rerollFree;
    }

    public void rerollFree(int rerollFree) {
        this.rerollFree = Math.max(0, rerollFree);
    }

    /** permission-suffix ("vip") -> rerolls per open; checked as nocrates.reroll.&lt;suffix&gt;. */
    public Map<String, Integer> rerollGroups() {
        return rerollGroups;
    }

    public void rerollGroups(Map<String, Integer> rerollGroups) {
        this.rerollGroups = new LinkedHashMap<>(rerollGroups);
    }

    /** Per-crate broadcast override; null uses the language default. */
    public String broadcastMessage() {
        return broadcastMessage;
    }

    public void broadcastMessage(String broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }
}
