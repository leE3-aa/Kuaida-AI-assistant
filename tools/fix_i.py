import pathlib

P = pathlib.Path('core/data/src/main/java/com/kuaida/core/data/di/DataModule.kt')
s = P.read_text(encoding='utf-8')

O1 = """import dagger.multibindings.IntoSet
import javax.inject.Singleton"""
N1 = """import dagger.multibindings.IntoSet
import java.util.concurrent.TimeUnit
import javax.inject.Singleton"""
assert s.count(O1) == 1, 'O1'
s = s.replace(O1, N1, 1)

O2 = """        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()"""
N2 = """        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()"""
assert s.count(O2) == 1, 'O2'
s = s.replace(O2, N2, 1)

P.write_text(s, encoding='utf-8')
print('ok i', len(s))
