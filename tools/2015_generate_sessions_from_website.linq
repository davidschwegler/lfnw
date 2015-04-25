<Query Kind="Program">
  <Reference>C:\Users\David\Downloads\Json60r8\Bin\Net45\Newtonsoft.Json.dll</Reference>
  <Reference>&lt;RuntimeDirectory&gt;\System.Net.Http.dll</Reference>
  <Namespace>Newtonsoft.Json.Linq</Namespace>
  <Namespace>Newtonsoft.Json</Namespace>
  <Namespace>System.Net.Http</Namespace>
</Query>

async void Main()
{
	string s = File.ReadAllText(@"C:\Code\Android\LinuxFestNorthwestExtras\Linq 2015\raw_sessions.json");
	
	var nodes = Newtonsoft.Json.JsonConvert.DeserializeObject<Sessions>(s).nodes;
		
	nodes.Count.Dump();
		
		List<string> httpFails = new List<string>();
		List<string> roomFails = new List<string>();
		List<NodeOut> newNodes = new List<NodeOut>();
	foreach(var node in nodes.Select(x=>x.node))
	{
	var replaceWords = new []{"the", "of", "a", "/", "--", "in", "with", "&", ":", "+", "an", "to", "from", "on", "is", "-", "for", "via", "as", "at"};
	string session = node.Session;
	foreach(var replace in replaceWords.SelectMany(x=> new []{x, (x[0].ToString().ToUpper() + x.Substring(1))}))
	{
		if(session.StartsWith(replace + " "))
			session = session.Substring(replace.Length + 1);
		session = session.Replace(" " + replace + " ", " ");
	}
	session.Dump();
	
	session = session.Replace("  ", " ");
	string urlTitle = Regex.Replace(session, "[^0-9A-Za-z\\-– ]", "").Trim().Replace(' ', '-');
		string url = "http://www.linuxfestnorthwest.org/2015/sessions/"  + urlTitle;
		url.Dump();
		
	HttpClient client = new HttpClient();
	string content = await client.GetStringAsync(url);
	if(content == null)
	 {
	 "HTTP FAIL^".Dump();
	 httpFails.Add(url);
	 //continue;
	 }
	//content.Dump();
	
	var roomToken = "field field--name-field-timeslot-room field--type-entityreference field--label-above\"><div class=\"field__label\">Room:&nbsp;</div><div class=\"field__items\"><div class=\"field__item even\">";
	
	var lines = content.Split(new []{'\n'});
	var roomLine = lines.FirstOrDefault(x=>x.Contains(roomToken));
	
	if(roomLine == null)
	{
	 "ROOM TOKEN FAIL^".Dump();
	 roomFails.Add(url);
	 continue;
	 }
	 
	int roomStart = roomLine.IndexOf(roomToken) + roomToken.Length;
	var room = roomLine.Substring(roomStart, roomLine.IndexOf('<', roomStart) - roomStart);
	room.Dump();
	
	var timeToken = "<div class=\"field field--name-field-timeslot-time field--type-entityreference field--label-above\"><div class=\"field__label\">Time:&nbsp;</div><div class=\"field__items\"><div class=\"field__item even\">";
	var timeLine = lines.FirstOrDefault(x=>x.Contains(timeToken));
	int timeStart = timeLine.IndexOf(timeToken) + timeToken.Length;
	var time = timeLine.Substring(timeStart, timeLine.IndexOf('<', timeStart) - timeStart);
	var timeParts = time.Split();
	var day = timeParts[0];
	day.Dump();
	var startTime = timeParts[1].Substring(0, timeParts[1].IndexOf("-"));
	startTime.Dump();
	var endTime = timeParts[2];
	endTime.Dump();
	
	var shortLinkToken = "<link rel=\"shortlink\" href=\"/node/";
	var shortLink = lines.FirstOrDefault(x=>x.Contains(shortLinkToken));
	var nodeIdString = shortLink.Substring(shortLinkToken.Length, shortLink.IndexOf("\"", shortLinkToken.Length) - shortLinkToken.Length);
	nodeIdString.Dump();
	
	var trackToken = "<div class=\"field field--name-og-vocabulary field--type-entityreference field--label-hidden\"><div class=\"field__items\"><div class=\"field__item even\">";
	var trackLine = lines.FirstOrDefault(x=>x.Contains(trackToken));
	int trackStart = trackLine.IndexOf(trackToken) + trackToken.Length;
	var track = trackLine.Substring(trackStart, trackLine.IndexOf("</div", trackStart) - trackStart);
	track = track.Replace("&amp;", "&");
	track.Dump();
	
	
	
	//><div class="field__label">Time:&nbsp;</div><div class="field__items"><div class="field__item even">2015-04-25 11:30-2015-04-25 12:30</div></div></div><div class="field field--name-field-timeslot-room field--type-entityreference field--label-above"><div class="field__label">Room:&nbsp;</div><div class="field__items"><div class="field__item even">CC-236</div></div></div>  </div>
//</div>", "", RegexOptions.

	newNodes.Add(new NodeOut
		{
			day = DateTime.Parse(day).ToString("ddd, dd MMM yyyy"),
			field_experience = node.ExperienceLevel,
			field_session_track = track,
			field_speakers = node.Speakers,
			name = room,
			nid = nodeIdString,
			time = DateTime.Parse(startTime).ToString("h:mm tt") + " to " + DateTime.Parse(endTime).ToString("h:mm tt"),
			title = node.Session
		});
		
		"\n".Dump();
	}
	
	"HTTP".Dump();
	httpFails.ForEach(x=>x.Dump());
	"ROOM".Dump();
	roomFails.ForEach(x=>x.Dump());
	
	var sessionsOut = new SessionsOut{nodes = new NodeCollectionOut(newNodes.Select(x=>new NodeHostOut{node = x}).ToList())};
	var stringOut = Newtonsoft.Json.JsonConvert.SerializeObject(sessionsOut);
	stringOut.Dump();
}

public class Sessions
{
public NodeCollection nodes;
}

public class SessionsOut
{
public NodeCollectionOut nodes;
}

public class NodeCollection : List<NodeHost>
{
public NodeCollection(List<NodeHost> list)
:base(list)
	{
	}
}

public class NodeCollectionOut : List<NodeHostOut>
{
public NodeCollectionOut(List<NodeHostOut> list)
:base(list)
	{
	}
}

public class NodeHost
{
	public Node node;
}

public class NodeHostOut
{
	public NodeOut node;
}


public class Node
{
public string Session;

[JsonProperty("Speaker(s)")]
public string Speakers;
public string body;
[JsonProperty("Experience level")]
public string ExperienceLevel;
}

public class NodeOut
{
public string nid; 
public string title;
public string name;
public string day;
public string time;
public string field_speakers;
public string field_session_track;
public string field_experience;
}

// Define other methods and classes here
