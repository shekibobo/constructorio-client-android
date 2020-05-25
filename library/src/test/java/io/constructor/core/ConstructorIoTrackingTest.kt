package io.constructor.core

import android.content.Context
import io.constructor.data.local.PreferencesHelper
import io.constructor.data.memory.ConfigMemoryHolder
import io.constructor.data.model.Group
import io.constructor.test.createTestDataManager
import io.constructor.util.RxSchedulersOverrideRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class ConstructorIoTest {

    @Rule
    @JvmField val overrideSchedulersRule = RxSchedulersOverrideRule()

    private lateinit var mockServer: MockWebServer
    private var constructorIo = ConstructorIo
    private val ctx = mockk<Context>()
    private val preferencesHelper = mockk<PreferencesHelper>()
    private val configMemoryHolder = mockk<ConfigMemoryHolder>()

    @Before
    fun setup() {
        every { ctx.applicationContext } returns ctx
        every { preferencesHelper.apiKey } returns "copper-key"
        every { preferencesHelper.id } returns "wacko-the-guid"
        every { preferencesHelper.getSessionId(any(), any()) } returns 67
        every { preferencesHelper.defaultItemSection  } returns "Products"
        every { configMemoryHolder.autocompleteResultCount } returns null
        every { configMemoryHolder.testCellParams = any() } just Runs
        every { configMemoryHolder.userId } returns "player-three"
        every { configMemoryHolder.testCellParams } returns emptyList()

        mockServer = MockWebServer()
        mockServer.start()
        val config = ConstructorIoConfig("dummyKey", testCells = listOf("flavor" to "peaches", "topping" to "cream"))
        val dataManager = createTestDataManager(mockServer, preferencesHelper, configMemoryHolder, ctx)

        constructorIo.testInit(ctx, config, dataManager, preferencesHelper, configMemoryHolder)
    }

    @Test
    fun trackAutocompleteSelect() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackAutocompleteSelect("titanic", "tit", "Search Suggestions").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/select?autocomplete_section=Search%20Suggestions&original_query=tit&tr=click&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackAutocompleteSelectWithServerError() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackAutocompleteSelect("titanic", "tit", "Search Suggestions").test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/select?autocomplete_section=Search%20Suggestions&original_query=tit&tr=click&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackAutocompleteSelectWithTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackAutocompleteSelect("titanic", "tit", "Search Suggestions").test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/select?autocomplete_section=Search%20Suggestions&original_query=tit&tr=click&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackAutocompleteSelectWithOptionalParams() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackAutocompleteSelect("titanic", "tit", "Search Suggestions", Group("recommended", "123123"), "2346784").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/select?autocomplete_section=Search%20Suggestions&original_query=tit&tr=click&group%5Bgroup_id%5D=123123&group%5Bdisplay_name%5D=recommended&result_id=2346784&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt="
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchSubmit() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchSubmit("titanic", "tit", null).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/search?original_query=tit&tr=search&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchSubmit500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchSubmit("titanic", "tit", null).test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/search?original_query=tit&tr=search&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchSubmitTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchSubmit("titanic", "tit", null).test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/search?original_query=tit&tr=search&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSessionStart() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = constructorIo.trackSessionStartInternal().test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?action=session_start&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSessionStart500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSessionStartInternal().test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/behavior?action=session_start&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSessionStartTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSessionStartInternal().test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/behavior?action=session_start&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackConversion() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackConversion("titanic replica", "TIT-REP-1997", 89.00).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/conversion?name=titanic%20replica&customer_id=TIT-REP-1997&revenue=89.00&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackConversion500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackConversion("titanic replica", "TIT-REP-1997", 89.00).test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/conversion?name=titanic%20replica&customer_id=TIT-REP-1997&revenue=89.00&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackConversionTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackConversion("titanic replica", "TIT-REP-1997", 89.00).test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/conversion?name=titanic%20replica&customer_id=TIT-REP-1997&revenue=89.00&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultClick() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultClick("titanic replica", "TIT-REP-1997", "titanic").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/click_through?name=titanic%20replica&customer_id=TIT-REP-1997&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultClick500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultClick("titanic replica", "TIT-REP-1997", "titanic").test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/click_through?name=titanic%20replica&customer_id=TIT-REP-1997&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultClickTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultClick("titanic replica", "TIT-REP-1997", "titanic").test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/click_through?name=titanic%20replica&customer_id=TIT-REP-1997&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultClickWithOptionalParams() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultClick("titanic replica", "TIT-REP-1997", "titanic", "Products","3467632").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/titanic/click_through?name=titanic%20replica&customer_id=TIT-REP-1997&autocomplete_section=Products&result_id=3467632&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultLoaded() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultsLoaded("titanic", 10).test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?term=titanic&num_results=10&action=search-results&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultLoaded500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultsLoaded("titanic", 10).test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/behavior?term=titanic&num_results=10&action=search-results&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackSearchResultLoadedTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackSearchResultsLoaded("titanic", 10).test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/behavior?term=titanic&num_results=10&action=search-results&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackInputFocus() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackInputFocus("tita").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/behavior?term=tita&action=focus&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackInputFocus500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackInputFocus("tita").test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/behavior?term=tita&action=focus&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackInputFocusTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackInputFocus("tita").test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/behavior?term=tita&action=focus&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackPurchase() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackPurchase(arrayOf("TIT-REP-1997", "QE2-REP-1969"), 12.99, "ORD-1312343").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/purchase?customer_ids=TIT-REP-1997&customer_ids=QE2-REP-1969&revenue=12.99&order_id=ORD-1312343&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackPurchase500() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackPurchase(arrayOf("TIT-REP-1997", "QE2-REP-1969"), 12.99, "ORD-1312343").test()
        observer.assertError { true }
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/purchase?customer_ids=TIT-REP-1997&customer_ids=QE2-REP-1969&revenue=12.99&order_id=ORD-1312343&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackPurchaseTimeout() {
        val mockResponse = MockResponse().setResponseCode(500).setBody("Internal server error")
        mockResponse.throttleBody(0, 5, TimeUnit.SECONDS)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackPurchase(arrayOf("TIT-REP-1997", "QE2-REP-1969"), 12.99,"ORD-1312343").test()
        observer.assertError(SocketTimeoutException::class.java)
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/purchase?customer_ids=TIT-REP-1997&customer_ids=QE2-REP-1969&revenue=12.99&order_id=ORD-1312343&autocomplete_section=Products&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }

    @Test
    fun trackPurchaseWithOptionalParams() {
        val mockResponse = MockResponse().setResponseCode(204)
        mockServer.enqueue(mockResponse)
        val observer = ConstructorIo.trackPurchase(arrayOf("TIT-REP-1997", "QE2-REP-1969"), 12.99, "ORD-1312343", "Recommendations").test()
        observer.assertComplete()
        val request = mockServer.takeRequest()
        val path = "/autocomplete/TERM_UNKNOWN/purchase?customer_ids=TIT-REP-1997&customer_ids=QE2-REP-1969&revenue=12.99&order_id=ORD-1312343&autocomplete_section=Recommendations&key=copper-key&i=wacko-the-guid&ui=player-three&s=67&c=cioand-1.3.0&_dt=";
        assert(request.path.startsWith(path))
    }
}