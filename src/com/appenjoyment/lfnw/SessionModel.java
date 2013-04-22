package com.appenjoyment.lfnw;

public final class SessionModel
{
	public SessionModel(SessionData sessionData)
	{
		m_sessionData = sessionData;
	}

	public boolean isStarred()
	{
		return m_isStarred;
	}

	public void setStarred(boolean starred)
	{
		m_isStarred = starred;
	}

	public SessionData getSessionData()
	{
		return m_sessionData;
	}

	private boolean m_isStarred;
	private final SessionData m_sessionData;
}
