public class DataSourceWrapper
{
	private DataSource dataSource;
	private boolean isRunning;

	public DataSourceWrapper(DataSource dataSource, boolean isRunning)
	{
		this.dataSource = dataSource;
		this.isRunning = isRunning;
	}

	public DataSource getDataSource()
	{
		return dataSource;
	}

	public void setDataSource(DataSource dataSource)
	{
		this.dataSource = dataSource;
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