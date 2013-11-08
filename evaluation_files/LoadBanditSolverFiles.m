function [AllBanditInfo, AllBanditNames] = LoadBanditSolverFiles(directoryname, summaryOnly)
    AllBanditInfo = zeros(0,0);
    abn = cell(System.IO.Directory.GetDirectories(directoryname));
    
    AllBanditNames = zeros(0,0);
    %load all subdirectories
    
    %OPTIONS FOR PLOTTING
    loadSummaryOnly = summaryOnly;

    for bandits = 1:numel(abn)
        banditDir = abn(bandits);
        banditDir = strrep(banditDir, '\', '/'); 
        banditDir = banditDir{1};
        %AllAlphas = cell(System.IO.Directory.GetDirectories(banditDir));
        if isempty(strfind(banditDir, 'notUsed'))
            subs = dir(banditDir);
            strArray = java_array('java.lang.String', numel(subs([subs(:).isdir]))-2);

            count = 1;
            for sfc = 3:numel(subs)

                if subs(sfc).isdir
                        [Mfull, MCommStep, MUtilStep, MVisStep] = LoadAllFiles(strcat(banditDir, '/', subs(sfc).name));
                        [t1, t2] = CalculateStatsPerStep(MCommStep);
                        [t3, t4] = CalculateStatsPerStep(MUtilStep);
                        [t5, t6] = CalculateStatsPerStep(MVisStep);
                        [r1, r2] = CalculateStatsPerRun(MCommStep);
                        [r3, r4] = CalculateStatsPerRun(MUtilStep);
                        [r5, r6] = CalculateStatsPerRun(MVisStep);
                        
                        AllBanditInfo = [AllBanditInfo; mean(t1) mean(t2) mean(t3) mean(t4) mean(t5) mean(t6)];
                        strArray(count) = java.lang.String(strcat(banditDir, '#', subs(sfc).name));
                        count = count + 1;
                end
            end
            AllBanditNames = [AllBanditNames; cell(strArray)];
        end
    end
    
end