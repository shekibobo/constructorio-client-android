package io.constructor.core

import android.content.Context
import io.constructor.data.builder.SearchRequest
import io.constructor.data.local.PreferencesHelper
import io.constructor.data.memory.ConfigMemoryHolder
import io.constructor.data.model.common.VariationsMap
import io.constructor.test.createTestDataManager
import io.constructor.util.RxSchedulersOverrideRule
import io.constructor.util.TestDataLoader
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

class ConstructorIoSearchTest {

    @Rule
    @JvmField
    val overrideSchedulersRule = RxSchedulersOverrideRule()

    private lateinit var mockServer: MockWebServer
    private var constructorIo = ConstructorIo
    private val ctx = mockk<Context>()
    private val preferencesHelper = mockk<PreferencesHelper>()
    private val configMemoryHolder = mockk<ConfigMemoryHolder>()

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()

        every { ctx.applicationContext } returns ctx

        every { preferencesHelper.apiKey } returns "silver-key"
        every { preferencesHelper.id } returns "guapo-the-guid"
        every { preferencesHelper.serviceUrl } returns mockServer.hostName
        every { preferencesHelper.port } returns mockServer.port
        every { preferencesHelper.scheme } returns "http"
        every { preferencesHelper.defaultItemSection } returns "Products"
        every { preferencesHelper.getSessionId(any(), any()) } returns 92

        every { configMemoryHolder.autocompleteResultCount } returns null
        every { configMemoryHolder.userId } returns "player-two"
        every { configMemoryHolder.testCellParams } returns emptyList()
        every { configMemoryHolder.segments } returns emptyList()

        val config = ConstructorIoConfig("dummyKey")
        val dataManager = createTestDataManager(preferencesHelper, configMemoryHolder, ctx)

        constructorIo.testInit(ctx, config, dataManager, preferencesHelper, configMemoryHolder)
    }

    @Test
    fun getSearchResults() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("corn").test()
        observer.assertComplete().assertValue {
            it.get()!!.response?.results!!.size == 24
            it.get()!!.response?.results!![0].value == "Del Monte Fresh Cut Corn Whole Kernel Golden Sweet with Natural Sea Salt - 15.25 Oz"
            it.get()!!.response?.results!![0].data.id == "121150086"
            it.get()!!.response?.results!![0].data.imageUrl == "https://d17bbgoo3npfov.cloudfront.net/images/farmstand-121150086.png"
            it.get()!!.response?.results!![0].data.metadata?.get("price") == 2.29
            it.get()!!.response?.results!![0].matchedTerms!![0] == "corn"
            it.get()!!.response?.facets!!.size == 3
            it.get()!!.response?.facets!![0].displayName == "Brand"
            it.get()!!.response?.facets!![0].type == "multiple"
            it.get()!!.response?.groups!!.size == 1
            it.get()!!.response?.resultCount == 225
        }
        val request = mockServer.takeRequest()
        val path = "/search/corn?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithServerError() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("corn").test()
        observer.assertComplete().assertValue {
            it.networkError
        }
        val request = mockServer.takeRequest()
        val path = "/search/corn?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithTimeout() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockResponse.throttleBody(128, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("corn").test()
        observer.assertComplete().assertValue {
            it.isError
        }
        val request = mockServer.takeRequest()
        val path = "/search/corn?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithEmptyResponse() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response_empty.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("corn").test()
        observer.assertComplete().assertValue {
            it.get()!!.response?.results!!.isEmpty()
            it.get()!!.response?.facets!!.isEmpty()
            it.get()!!.response?.groups!!.isEmpty()
            it.get()!!.response?.resultCount == 0
        }
        val request = mockServer.takeRequest()
        val path = "/search/corn?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithRedirect() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response_redirect.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("bbq").test()
        observer.assertComplete().assertValue {
            it.get()!!.response?.redirect?.data?.url == "/mccormicks_farmstand.html?barbeque-and-grilling=1"
            it.get()!!.response?.redirect?.matchedTerms!![0] == "bbq"
        }
        val request = mockServer.takeRequest()
        val path = "/search/bbq?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithSection() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("bbq", null, null, null , null, null, null, "Sold Out").test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?section=Sold%20Out&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithFacets() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val facets = listOf(Pair("Brand", listOf("Signature Farms", "Del Monte")), Pair("Nutrition", listOf("Organic")))
        val observer = constructorIo.getSearchResults("bbq", facets, null, null , null, null, null, null).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?filters%5BBrand%5D=Signature%20Farms&filters%5BBrand%5D=Del%20Monte&filters%5BNutrition%5D=Organic&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1"
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithHiddenFields() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("bbq", null, null, null , null, null, null, null, listOf("hiddenField1", "hiddenField2")).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?fmt_options%5Bhidden_fields%5D=hiddenField1&fmt_options%5Bhidden_fields%5D=hiddenField2&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithHiddenFacets() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val hiddenFacets = listOf("Brand", "price_US")
        val observer = constructorIo.getSearchResults("bbq", null, null, null , null, null, null, null, null, hiddenFacets).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?fmt_options%5Bhidden_facets%5D=Brand&fmt_options%5Bhidden_facets%5D=price_US&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithProperUrlEncoding() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults("2% cheese").test()
        val request = mockServer.takeRequest()
        val path = "/search/2%25%20cheese?key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithGroupsSort() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getSearchResults(term = "bbq", groupsSortBy = "value", groupsSortOrder = "descending").test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?fmt_options%5Bgroups_sort_by%5D=value&fmt_options%5Bgroups_sort_order%5D=descending&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithFiltersUsingBuilder() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val filters = mapOf(
                "Brand" to listOf("Signature Farms", "Del Monte"),
                "Nutrition" to listOf("Organic")
        )
        val searchRequest = SearchRequest.Builder("bbq")
                .setFilters(filters)
                .build()
        val observer = constructorIo.getSearchResults(searchRequest).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?filters%5BBrand%5D=Signature%20Farms&filters%5BBrand%5D=Del%20Monte&filters%5BNutrition%5D=Organic&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1"
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithGroupsSortUsingBuilder() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val searchRequest = SearchRequest.Builder("bbq")
                .setGroupsSortBy("value")
                .setGroupsSortOrder("ascending")
                .build()
        val observer = constructorIo.getSearchResults(searchRequest).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?fmt_options%5Bgroups_sort_by%5D=value&fmt_options%5Bgroups_sort_order%5D=ascending&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }

    @Test
    fun getSearchResultsWithVariationsMapUsingBuilder() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("search_response.json"))
        mockServer.enqueue(mockResponse)
        val variationsMap = VariationsMap("array", mapOf("Price" to mapOf("aggregation" to "min", "field" to "data.facets.price"), "Country" to mapOf("aggregation" to "all", "field" to "data.facets.country")), listOf(mapOf("name" to "Country", "field" to "data.facets.Country")))
        val searchRequest = SearchRequest.Builder("bbq")
                .setVariationsMap(variationsMap)
                .build()
        val observer = constructorIo.getSearchResults(searchRequest).test()
        val request = mockServer.takeRequest()
        val path = "/search/bbq?variations_map=%7B%22dtype%22%3A%22array%22%2C%22group_by%22%3A%5B%7B%22name%22%3A%22Country%22%2C%22field%22%3A%22data.facets.Country%22%7D%5D%2C%22values%22%3A%7B%22Price%22%3A%7B%22aggregation%22%3A%22min%22%2C%22field%22%3A%22data.facets.price%22%7D%2C%22Country%22%3A%7B%22aggregation%22%3A%22all%22%2C%22field%22%3A%22data.facets.country%22%7D%7D%7D&key=silver-key&i=guapo-the-guid&ui=player-two&s=92&c=cioand-2.17.1&_dt="
        assert(request.path!!.startsWith(path))
    }
}