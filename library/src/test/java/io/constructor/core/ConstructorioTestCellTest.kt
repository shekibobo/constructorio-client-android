package io.constructor.core

import android.content.Context
import io.constructor.data.local.PreferencesHelper
import io.constructor.data.memory.ConfigMemoryHolder
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
import kotlin.test.assertTrue

class ConstructorioTestCellTest {

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

        every { preferencesHelper.apiKey } returns "aluminium-key"
        every { preferencesHelper.id } returns "koopa-the-guid"
        every { preferencesHelper.serviceUrl } returns mockServer.hostName
        every { preferencesHelper.port } returns mockServer.port
        every { preferencesHelper.scheme } returns "http"
        every { preferencesHelper.defaultItemSection } returns "Products"
        every { preferencesHelper.getSessionId(any(), any()) } returns 14

        every { configMemoryHolder.autocompleteResultCount } returns null
        every { configMemoryHolder.userId } returns "player-two"
        every { configMemoryHolder.testCellParams } returns listOf("cellone" to "vanilla", "celltwo" to "whipped-cream")
        every { configMemoryHolder.segments } returns  emptyList()


        val config = ConstructorIoConfig("dummyKey")
        val dataManager = createTestDataManager(preferencesHelper, configMemoryHolder, ctx)

        constructorIo.testInit(ctx, config, dataManager, preferencesHelper, configMemoryHolder)
    }

    @Test
    fun getAutocompleteResults() {
        val mockResponse = MockResponse().setResponseCode(200).setBody(TestDataLoader.loadAsString("autocomplete_response.json"))
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.getAutocompleteResults("titanic").test()
        observer.assertComplete().assertValue {
            var suggestions = it.get()!!.sections?.get("Search Suggestions");
            suggestions?.isNotEmpty()!! && suggestions.size == 5
        }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic?key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSessionStart() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.trackSessionStartInternal().test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?action=session_start&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackInputFocus() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackInputFocusInternal("tita").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?term=tita&action=focus&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackAutocompleteSelect() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackAutocompleteSelectInternal("titanic", "tit", "Search Suggestions").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/select?section=Search%20Suggestions&original_query=tit&tr=click&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchSubmit() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchSubmitInternal("titanic", "tit", null).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/search?original_query=tit&tr=search&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultLoaded() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultsLoadedInternal("titanic", 10).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?term=titanic&num_results=10&action=search-results&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultClick() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultClickInternal("titanic replica", "TIT-REP-1997", "titanic").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/click_through?name=titanic%20replica&customer_id=TIT-REP-1997&section=Products&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackConversion() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackConversionInternal("titanic replica", "TIT-REP-1997", 89.00).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/v2/behavioral_action/conversion?key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackPurchase() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackPurchaseInternal(arrayOf("TIT-REP-1997", "QE2-REP-1969"), 12.99, "ORD-1312343").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/v2/behavioral_action/purchase?section=Products&key=aluminium-key&i=koopa-the-guid&ui=player-two&s=14&ef-cellone=vanilla&ef-celltwo=whipped-cream&c=cioand-2.9.0&_dt=";
        assertTrue(request.bodySize > 230)
        assert(request.path.startsWith(path))
    }
}
