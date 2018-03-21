package br.com.uhunter;

import javax.ws.rs.*;

import org.jsoup.Jsoup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.uhunter.model.JsoupRun;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Path("/urest/")
public class UsabilityRest {

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	@Path("/runtest")
	public String setTest(String content) {
		Gson gson = new Gson();
		try {

			Type type = new TypeToken<Map<String, String>>() {
			}.getType();

			Map<String, String> map = gson.fromJson(content, type);
			String url = map.get("url");
	

			return gson.toJson(JsoupRun.getLogoPage(url));

		} catch (Exception e) {
			e.printStackTrace();
			return gson.toJson("ServerResponse: Error: " + e.getMessage());

		}

	}

	@GET
	@Produces("application/json")
	@Path("/gettest")
	public String getTest() {

		Gson gson = new Gson();

		try {
			return gson.toJson("It worked!");

		} catch (Exception e) {
			return gson.toJson("ServerResponse: Erro: " + e.getMessage());
		}
	}
}
