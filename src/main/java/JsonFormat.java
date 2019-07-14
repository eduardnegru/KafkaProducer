import java.util.ArrayList;
import java.util.List;

public class JsonFormat
{
	ArrayList<SourceState> arrDataSourceState;

	public JsonFormat(ArrayList<SourceState> dataSourceJsons)
	{
		this.arrDataSourceState = dataSourceJsons;
	}

	public ArrayList<SourceState> getDataSources()
	{
		return arrDataSourceState;
	}

	public void setDataSource(SourceState dataSourceJson)
	{
		this.arrDataSourceState.add(dataSourceJson);
	}
}

class SourceState
{
	private String dataSourceName;
	private boolean isRunning;
	private String tag;

	public SourceState(String dataSourceName, boolean isRunning)
	{
		this.dataSourceName = dataSourceName;
		this.isRunning = isRunning;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}

	public String getTag()
	{
		return tag;
	}

	public void setTag(String tag)
	{
		this.tag = tag;
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	public void setRunning(boolean running)
	{
		isRunning = running;
	}
}