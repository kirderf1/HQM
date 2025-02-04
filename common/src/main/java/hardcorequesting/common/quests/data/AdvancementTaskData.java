package hardcorequesting.common.quests.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hardcorequesting.common.io.adapter.Adapter;
import hardcorequesting.common.io.adapter.QuestTaskAdapter;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class AdvancementTaskData extends TaskData {
    
    private static final String COUNT = "count";
    private static final String ADVANCED = "advanced";
    private final List<Boolean> advanced;
    
    public AdvancementTaskData(int size) {
        super();
        this.advanced = new ArrayList<>(size);
        while (advanced.size() < size) {
            advanced.add(false);
        }
    }
    
    public static TaskData construct(JsonObject in) {
        AdvancementTaskData data = new AdvancementTaskData(GsonHelper.getAsInt(in, COUNT));
        data.completed = GsonHelper.getAsBoolean(in, COMPLETED, false);
        JsonArray array = GsonHelper.getAsJsonArray(in, ADVANCED);
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).getAsBoolean())
                data.complete(i);
        }
        return data;
    }
    
    public boolean getValue(int id) {
        if (id >= advanced.size())
            return false;
        else return advanced.get(id);
    }
    
    public void complete(int id) {
        while (id >= advanced.size()) {
            advanced.add(false);
        }
        advanced.set(id, true);
    }
    
    public void mergeResult(AdvancementTaskData other) {
        for (int i = 0; i < other.advanced.size(); i++) {
            if (other.advanced.get(i))
                complete(i);
        }
    }
    
    public float getCompletedRatio(int size) {
        return (float) advanced.stream().limit(size).filter(Boolean::booleanValue).count() / size;
    }
    
    public boolean areAllCompleted(int size) {
        return advanced.stream().limit(size).allMatch(Boolean::booleanValue);
    }
    
    @Override
    public QuestTaskAdapter.QuestDataType getDataType() {
        return QuestTaskAdapter.QuestDataType.ADVANCEMENT;
    }
    
    @Override
    public void write(Adapter.JsonObjectBuilder builder) {
        super.write(builder);
        builder.add(COUNT, advanced.size());
        builder.add(ADVANCED, Adapter.array(advanced.toArray()).build());
    }
    
    @Override
    public void update(TaskData taskData) {
        super.update(taskData);
        this.advanced.clear();
        this.advanced.addAll(((AdvancementTaskData) taskData).advanced);
    }
}

