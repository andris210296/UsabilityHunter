package br.com.uhunter;

import javax.ws.rs.*;

import org.jsoup.Jsoup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.uhunter.model.UsabilityRunTests;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Path("/urest/")
public interface UsabilityRest {

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/runtest")
	public String setTest(String content);

	@GET
	@Produces("application/json")
	@Path("/gettest")
	public String getTest();
}
